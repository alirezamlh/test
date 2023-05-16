package controller;

import model.*;

import Enum.Resource;

import java.util.regex.Matcher;

public class ShopController extends GameController{

    public ShopController(Game game) {
        super(game);
    }

    public String showPriceList() {
        String output = "resources: ";
        for (Resource value : Resource.values()) {
            output += "\n" + value.name().toLowerCase() + "- price: " + value.buyPrice
                    + " sell price: " + value.sellPrice;
        }
        return output;
    }
    public String purchase(Matcher matcher) {
        int amount = Integer.parseInt(matcher.group("amount"));
        if(amount <= 0) return "the amount is not correct";
        Resource resource = Resource.getResourceByName(matcher.group("item"));
        if(resource == null) return "resource not found in the shop";
        if(playingReign.getResourceCapacity(resource) < amount + playingReign.getResourceAmount(resource))
            return "you don't have enough capacity";
        if(amount * resource.buyPrice > playingReign.getGold()) return "you don't have enough gold";
        playingReign.changeResourceAmount(resource, amount);
        return "you have bought the resources successfully";
    }
    public String sell(Matcher matcher) {
        int amount = Integer.parseInt(matcher.group("amount"));
        if(amount <= 0) return "the amount is not correct";
        Resource resource = Resource.getResourceByName(matcher.group("item"));
        if(resource == null) return "resource not found in the shop";
        if(amount < playingReign.getResourceAmount(resource)) return "you don't have enough resources to sell";
        playingReign.spendGold(amount * resource.sellPrice);
        playingReign.changeResourceAmount(resource , -amount);
        return "selling resources successful";
    }

}
