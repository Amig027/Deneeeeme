package org.fivedolladubs;

import java.util.ArrayList;

public class Customer {
    private final long userID;
    private final ArrayList<Item> cart;

    public Customer(long userID) {
        cart = new ArrayList<>();
        this.userID = userID;
    }


    public ArrayList<Item> getCart() {
        return cart;
    }

    public long getUserID() {
        return userID;
    }

    public void addItem(Item item) {
        cart.add(item);
    }

    public void removeItem(Item item) {
        cart.remove(item);
    }
    public void clearCart() {
        cart.clear();
    }
}

