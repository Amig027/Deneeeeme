package org.fivedolladubs;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kotlin.random.Random;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.GenericMessageEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.restaction.CommandListUpdateAction;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.HashMap;
import java.util.Map;



public class ShopBot extends ListenerAdapter {

    private static final LinkedHashSet<Customer> customerHashSet = new LinkedHashSet<>();

    private final String token;
    private final String categoryID;
    private final String roleId;


    public ShopBot(String token, String channelId, String roleId) {
        this.token = token;
        this.categoryID = channelId;
        this.roleId = roleId;
    }
private Customer getCustomerById(long userId) {
    for (Customer customer : customerHashSet) {
        if (customer.getUserID() == userId) {
            return customer;
        }
    }
    return null;
}   


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {


        if (!event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(roleId))) {
            event.reply("You don't have the required role to use this command!")
                    .setEphemeral(true)
                    .queue();
            return;
        }
        if (event.getInteraction().getName().equals("close_ticket")) {


            try {
                String filePath = event.getChannel().getName() + "_" + event.getTimeCreated().toString().replace(":", "-") + ".txt";
                System.out.println(filePath);
                new File("./logs/", filePath).mkdirs();
                new File("./logs/", filePath).createNewFile();
                PrintWriter writer = new PrintWriter(filePath);

                event.getChannel().getHistory().retrievePast(100).complete().forEach(msg -> {
                    String message = "[" + msg.getTimeCreated() + "] " + msg.getContentRaw();
                    writer.println(message);
                });

                writer.flush();
                writer.close();
                System.out.println("File written successfully.");
            } catch (FileNotFoundException e) {
                System.out.println("Error: File not found.");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            event.reply("Closing ticket...")
                    .setEphemeral(true)
                    .queue();


            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            event.getChannel().delete().queue();

            return;
        }
       
        if (event.getInteraction().getName().equals("clearcart")) {
            long userId = event.getUser().getIdLong();
            Customer customer = getCustomerById(userId);
    
            if (customer != null) {
                customer.clearCart();
                event.reply("Your cart has been cleared successfully!").setEphemeral(true).queue();
            } else {
                event.reply("You don't have a cart to clear!").setEphemeral(true).queue();
            }
    
            return;
        }
        
        if (event.getName().equals("register_item")) {
            final String itemName = event.getOption("name").getAsString();
            final String imageUrl = event.getOption("thumbnail").getAsString();
            final double price = Double.parseDouble(event.getOption("price").getAsString());
            final String hexCode = event.getOption("hex_code").getAsString().replace("#", "");


            Item item = new Item(itemName, price, imageUrl);

            int hexColor = Integer.parseInt(hexCode, 16);

            MessageEmbed embed = generateItemEmbed(item, hexColor);
            event.getChannel().sendMessageEmbeds(embed).setActionRow(
                    Button.success("add_to_cart", "Add to Cart"),
                    Button.danger("remove_from_cart", "Remove from Cart"),
                    Button.secondary("checkout", "Checkout")
            ).queue();
        } else {
            event.reply("Unknown command!").setEphemeral(true).queue();
        }
    }
//Add random hex color if hex is blank 
private MessageEmbed generateItemEmbed(Item item, Integer hexColor) {
    int color = (hexColor != null && hexColor >= 0x1000) ? hexColor : generateRandomHexColor();

    return new EmbedBuilder()
            .setTitle(item.getName())
            .setDescription("Item added successfully!")
            .addField("Price", String.valueOf(item.getPrice()), true)
            .setImage(item.getImageUrl())
            .setColor(new Color(color))
            .build();
}

private int generateRandomHexColor() {
    // Generate a random hex color (e.g., #RRGGBB) and ensure it has at least 4 digits
    int randomColor = Random.Default.nextInt(0xFFFFFF + 1);
    while (randomColor < 0x1000) {
        randomColor = Random.Default.nextInt(0xFFFFFF + 1);
    }
    return randomColor;
}
// Helper method to get the price of a specific item
private double getItemPrice(String itemName, List<Item> cart) {
    return cart.stream()
            .filter(item -> item.getName().equals(itemName))
            .mapToDouble(Item::getPrice)
            .sum();
}
// Helper method to format the item list with item occurrences
private String formatItemList(List<Item> cart) {
    Map<String, Integer> itemOccurrences = new HashMap<>();

    // Count occurrences of each item
    for (Item item : cart) {
        String itemName = item.getName();
        itemOccurrences.put(itemName, itemOccurrences.getOrDefault(itemName, 0) + 1);
    }

    // Build the formatted item list
    StringBuilder itemList = new StringBuilder();
    itemList.append("Items In Cart:\n");

    for (Map.Entry<String, Integer> entry : itemOccurrences.entrySet()) {
        itemList.append(entry.getKey()).append(" x").append(entry.getValue()).append(" \n");
    }


    return itemList.toString();
}

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        final long userId = event.getMember().getIdLong();
        final TextChannel channel = event.getChannel().asTextChannel();

        if (event.getComponentId().equals("checkout")) {
            if (!event.getMember().getRoles().stream().anyMatch(role -> role.getId().equals(roleId))) {
                event.reply("You don't have the required role to perform the checkout!")
                        .setEphemeral(true)
                        .queue();
                return;
            }
            Optional<Customer> optionalCustomer = customerHashSet.stream()
                    .filter(customer -> customer.getUserID() == userId)
                    .findFirst();


            if (optionalCustomer.isEmpty()) {
                event.reply("ur cart is empty bruh")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            ArrayList<Item> cart = optionalCustomer.get().getCart();


            if (optionalCustomer.get().getCart().isEmpty() || optionalCustomer.get().getCart().get(0) == null) {
                event.reply("ur cart is empty bruh")
                        .setEphemeral(true)
                        .queue();
                return;
            }

            Optional<TextChannel> optionalChannel = event.getGuild().getTextChannels().stream()
                    .filter(textChannel -> textChannel.getName().equals(String.valueOf(userId)))
                    .findFirst();


            if (optionalChannel.isEmpty()) {
                channel.createCopy().setName(String.valueOf(userId)).setParent(event.getGuild().getCategoryById(this.categoryID))
                        .addPermissionOverride(event.getMember(), List.of(Permission.VIEW_CHANNEL), null)
                        .addPermissionOverride(event.getGuild().getPublicRole(), null, List.of(Permission.VIEW_CHANNEL))
                        .queue(copy -> event.reply("Checkout initiated. Please proceed to the checkout channel: " + copy.getAsMention())
                                .setEphemeral(true)
                                .queue());
            }

            new Thread(() -> {
                try {
                    Thread.sleep(1500);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                //shitcode hehe
                Optional<TextChannel> optionalChannel2 = event.getGuild().getTextChannels().stream()
                        .filter(textChannel -> textChannel.getName().equals(String.valueOf(userId)))
                        .findFirst();


                TextChannel textChannel = optionalChannel2.get();

//          Price Calculation
double price = 0;

for (Item item : cart) {
    price += item.getPrice();
}
//                System.out.println(textChannel.getName());
        textChannel.sendMessage(formatItemList(cart) + "\nTotal Price: $" + price).queue();


            }).start();

        } else if (event.getComponentId().equals("add_to_cart")) {
            // find or create customer
            Optional<Customer> optionalCustomer = customerHashSet.stream()
                    .filter(customer -> customer.getUserID() == userId)
                    .findFirst();

            Customer customer = optionalCustomer.orElseGet(() -> {
                Customer newCustomer = new Customer(userId);
                customerHashSet.add(newCustomer);
                return newCustomer;
            });

            Item item = getItemFromMessage(event.getMessage());
            if (item != null) {
                customer.addItem(item);
                event.reply("Item added to cart!").setEphemeral(true).queue();
            } else {
                event.reply("Failed to add item to cart. Item information not found.").setEphemeral(true).queue();
            }
        } else if (event.getComponentId().equals("remove_from_cart")) {
            // find customer
            Optional<Customer> optionalCustomer = customerHashSet.stream()
                    .filter(customer -> customer.getUserID() == userId)
                    .findFirst();
            if (optionalCustomer.isPresent()) {
                Customer customer = optionalCustomer.get();

                customer.getCart().remove(0);
                event.reply("Item removed from cart!").setEphemeral(true).queue();
            }
        } else {
            event.reply("Failed to remove item from cart. Customer not found.").setEphemeral(true).queue();
        }
    }


    private Item getItemFromMessage(Message message) {
        String itemName = null;
        double itemPrice = 0.0;
        String imageUrl = null;

        // get item information from the message
        List<MessageEmbed> embeds = message.getEmbeds();
        if (!embeds.isEmpty()) {
            MessageEmbed embed = embeds.get(0);
            itemName = embed.getTitle();
            String priceString = embed.getFields().get(0).getValue();
            try {
                itemPrice = Double.parseDouble(priceString);
            } catch (NumberFormatException e) {
                System.out.println("Failed to parse item price: " + priceString);
                return null;
            }
            imageUrl = embed.getImage().getUrl();
        }


        // Create and return an Item object
        if (itemName != null && imageUrl != null) {
            return new Item(itemName, itemPrice, imageUrl);
        } else {
            return null;
        }
    }


    @Override
    public void onGenericMessage(GenericMessageEvent event) {
        CommandListUpdateAction commands = event.getGuild().updateCommands();


        commands.addCommands(
                Commands.slash("register_item", "Adds an item to the shop.")
                        .addOption(OptionType.STRING, "name", "The name of the item.")
                        .addOption(OptionType.STRING, "thumbnail", "The URL of the item thumbnail.")
                        .addOption(OptionType.STRING, "price", "The price of the item.")
                        .addOption(OptionType.STRING, "hex_code", "The hex code for the item color."),
                Commands.slash("close_ticket", "Closes a ticket where executed"),
                Commands.slash("clearcart", "Clears Cart")
        );

        commands.queue();

    }
        
    public void start() throws LoginException {
        JDABuilder.createDefault(token).addEventListeners(this).build();
    }

    public static void main(String[] args) {
        String configPath = "config.json";
        try (FileReader reader = new FileReader(configPath)) {
            JsonObject jsonConfig = JsonParser.parseReader(reader).getAsJsonObject();
            String token = jsonConfig.get("token").getAsString();
            String categoryId = jsonConfig.get("ticketCategoryId").getAsString();
            String roleId = jsonConfig.get("roleId").getAsString();

            ShopBot shopBot = new ShopBot(token, categoryId, roleId);
            shopBot.start();
        } catch (IOException | LoginException e) {
            e.printStackTrace();
        }
    }

    private static void startDebugPrintThread() {
        Thread debugThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000); // Sleep for 1 second
                    System.out.println("Customers:");
                    for (Customer customer : customerHashSet) {
                        System.out.println("Customer ID: " + customer.getUserID());
                        System.out.println("Items in Cart:");
                        for (Item item : customer.getCart()) {
                            System.out.println("- " + item.getName() + " (Price: " + item.getPrice() + ")");
                        }
                        System.out.println("----------------------");
                    }
                    System.out.println("======================");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        debugThread.setDaemon(true);
        debugThread.start();
    }

}