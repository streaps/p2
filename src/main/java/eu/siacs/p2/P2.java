package eu.siacs.p2;

import eu.siacs.p2.controller.PushController;
import org.apache.commons.cli.*;
import rocks.xmpp.core.XmppException;
import rocks.xmpp.core.session.XmppSessionConfiguration;
import rocks.xmpp.core.session.debug.ConsoleDebugger;
import rocks.xmpp.extensions.commands.model.Command;
import rocks.xmpp.extensions.component.accept.ExternalComponent;
import rocks.xmpp.extensions.pubsub.model.PubSub;

import java.io.FileNotFoundException;
import java.security.SecureRandom;

public class P2 {

    private static final Options options;
    public static SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final int RETRY_INTERVAL = 5000;

    static {
        options = new Options();
        options.addOption(new Option("c", "config", true, "Path to the config file"));
    }

    public static void main(String... args) {
        try {
            main(new DefaultParser().parse(options, args));
        } catch (ParseException e) {
            System.err.println(e.getMessage());
        }
    }

    private static void main(CommandLine commandLine) {
        String config = commandLine.getOptionValue('c');
        if (config != null) {
            try {
                Configuration.setFilename(config);
            } catch (FileNotFoundException e) {
                System.err.println("The config file you supplied does not exits");
                return;
            }
        }

        final XmppSessionConfiguration.Builder builder = XmppSessionConfiguration.builder();

        if (Configuration.getInstance().isDebug()) {
            builder.debugger(ConsoleDebugger.class);
        }

        final ExternalComponent externalComponent = ExternalComponent.create(
                Configuration.getInstance().getName(),
                Configuration.getInstance().getSharedSecret(),
                builder.build(),
                Configuration.getInstance().getHost(),
                Configuration.getInstance().getPort()
        );

        externalComponent.addIQHandler(Command.class, PushController.register);
        externalComponent.addIQHandler(PubSub.class, PushController.push);

        connectAndKeepRetrying(externalComponent);
    }

    private static void connectAndKeepRetrying(final ExternalComponent component) {
        while (true) {
            try {
                component.connect();
                while (component.isConnected()) {
                    Utils.sleep(500);
                }
            } catch (XmppException e) {
                System.err.println(e.getMessage());
            }
            Utils.sleep(RETRY_INTERVAL);
        }
    }

}
