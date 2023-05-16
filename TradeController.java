package controller;

import model.*;
import Enum.*;
import view.TradeMenu;

import java.util.regex.*;

public class TradeController extends GameController{
    private Reign secondReign;

    public TradeController(Game game) {
        super(game);
    }

    public String showTradeList () {
        return playingReign.showTradeList()  + '\n' + TradeItem.showTradeList();
    }
    public String showMyRequestsFromOthers () {
        String output = "requests form other reigns: ";
        for (TradeItem request : playingReign.getRequestsFromOthers()) {
            output += "\n" + "product: " + request.getResource().name().toLowerCase()
                    + "amount: " + request.getAmount()
                    + "price" + request.getPrice();
            if(request.getSecondReign() != null) output += "from " + request.getSecondReign().getNickName();
        }
        return output;
    }
    public String showMembers() {
        return game.showReigns();
    }
    public String chooseSecondReign() {
        while(true) {
            String entry = TradeMenu.getReignFromUser();
            if(entry.matches("back")) return "back";
            secondReign = game.getReignByNickName(entry);
            if(secondReign == null) TradeMenu.nickNameNotFound();
            else return "found";
        }
    }

    public String addRequest(Matcher matcher) {
        int amount = Integer.parseInt(matcher.group("amount"));
        int price = Integer.parseInt(matcher.group("price"));
        if(price <= 0) return "you cant add a request with no price";
        if(!hasEnoughBalance(price)) return "you don't have enough balance";
        Resource resource = Resource.getResourceByName(matcher.group("type"));
        if(resource == null) return "you have entered the wrong resource";
        TradeItem tradeItem = new TradeItem(playingReign, secondReign, resource, amount , price , matcher.group("message"));
        playingReign.spendGold(price);
        TradeItem.getTradeList().add(0 , tradeItem);
        playingReign.getRequestsFromOthers().add(0 , tradeItem);
        if(secondReign != null) {
            secondReign.getNotification().add(0 , tradeItem);
            secondReign.getRequestsFromMe().add(0 , tradeItem);
        }
        return "add request successful";
    }


    public String acceptTrade(Matcher matcher) {
        int id = Integer.parseInt(matcher.group("id"));
        TradeItem tradeItem = TradeItem.getTradeItemById(id);
        if(tradeItem == null) return "this item does not exist in the list";
        secondReign = tradeItem.getFirstReign();
        if(tradeItem.getSecondReign().equals(playingReign)) return "this request is not from you";
        if(playingReign.getResourceAmount(tradeItem.getResource()) < tradeItem.getAmount())
            return "you don't have enough resource to give" + secondReign.getNickName();
        tradeItem.setMessage(matcher.group("message"));
        playingReign.earnGold(tradeItem.getPrice());
        playingReign.changeResourceAmount(tradeItem.getResource(), -tradeItem.getAmount());
        secondReign.changeResourceAmount(tradeItem.getResource(), tradeItem.getAmount());
        playingReign.getRequestsFromMe().remove(tradeItem);
        secondReign.getRequestsFromOthers().remove(tradeItem);
        playingReign.getTradeHistory().add(tradeItem);
        secondReign.getTradeHistory().add(tradeItem);
        TradeItem.getTradeList().remove(tradeItem);
        secondReign.getNotification().add(tradeItem);
        secondReign = null;
        return "the trade was accepted successfully";
    }
    public String deleteTrade(Matcher matcher) {
        int id = Integer.parseInt(matcher.group("id"));
        TradeItem tradeItem = TradeItem.getTradeItemById(id);
        if(tradeItem == null) return "this item does not exist";
        if(!tradeItem.getFirstReign().equals(playingReign)) return "you did not add this request";
        TradeItem.getTradeList().remove(tradeItem);
        playingReign.getRequestsFromOthers().remove(tradeItem);
        secondReign = tradeItem.getSecondReign();
        if(secondReign != null) {
            secondReign.getNotification().remove(tradeItem);
            secondReign.getRequestsFromMe().remove(tradeItem);
        }
        playingReign.earnGold(tradeItem.getPrice());
        return "your request was successfully removed";
    }
    public String donate(Matcher matcher) {
        int amount = Integer.parseInt(matcher.group("amount"));
        if (amount <= 0) return "you can't donate resources with zero amount";
        Resource resource = Resource.getResourceByName(matcher.group("type"));
        if(resource == null) return "you have entered the wrong resource";
        if(playingReign.getResourceAmount(resource) < amount) return "you don't have enough resources";
        TradeItem tradeItem = new TradeItem(playingReign, secondReign, resource, amount , 0 , matcher.group("message"));
        secondReign.getNotification().add(0 , tradeItem);
        playingReign.getTradeHistory().add(0 , tradeItem);
        secondReign.getTradeHistory().add(0 , tradeItem);
        return "donation successful";
    }
    public String showTradeHistory() {
        return "Donations: \n" + playingReign.getHistoryOfTrades(true)
                + "\n Trades: \n" + playingReign.getHistoryOfTrades(false);
    }
    public String notification() {
        return "Donations: \n" + playingReign.showNotification(true)
                + "\n Trades: \n" + playingReign.showNotification(false);
    }
    public void clearNotification() {
        playingReign.clearNotification();
    }

    public void deleteSecondReign() {
        secondReign = null;
    }

    public boolean hasEnoughBalance(int price) {
        if(price > playingReign.getGold()) return false;
        return true;
    }

}
