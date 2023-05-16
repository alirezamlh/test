package controller;

import model.*;
import Enum.*;

import java.util.regex.Matcher;

public class ReignController extends GameController{
    public ReignController(Game game) {
        super(game);
    }

    public String showPopularityFactors() {
        return showFoodList() + "\n" + showFoodRate() + "\n" + showTaxRate() + "\n" + showFearRate();
    }

    public String showFearRate() {
        return "Your fear rate: "+game.getPlayingReign().getFearRate();

    }

    public String showPopularity() {
        return "Your popularity is: "+game.getPlayingReign().getPopularity();
    }
    public String showFoodList() {
        String stringBuilder = "Your food situation: \n" + "Bread : " + game.getPlayingReign().getResourceAmount(Resource.BREAD) +
                "\nApple : " + game.getPlayingReign().getResourceAmount(Resource.APPLE) +
                "\nCheese : " + game.getPlayingReign().getResourceAmount(Resource.CHEESE) +
                "\nMeat : " + game.getPlayingReign().getResourceAmount(Resource.MEAT);
        return stringBuilder;

    }
    public String setFoodRate(Matcher matcher) {
        int rate = Integer.parseInt(matcher.group("rate"));
        if(rate > 2 || rate < -2)
            return "rate not valid!";
        game.getPlayingReign().setFoodRate(rate);
        return "food rate set!";
    }
    public String showFoodRate() {
        return "Your food rate: "+game.getPlayingReign().getFoodRate();
    }
    public String setTaxRate(Matcher matcher) {
       int rate = Integer.parseInt(matcher.group("rate"));
        if(rate > 8 || rate < -3)
            return "rate not valid!";
        game.getPlayingReign().setTaxRate(rate);
        return "tax rate set!";
    }
    public String showTaxRate() {
        return "Your tax rate: "+game.getPlayingReign().getTaxRate();
    }
    public String setFearRate(Matcher matcher) {
        int rate = Integer.parseInt(matcher.group("rate"));
        if(rate > 6|| rate < -6)
            return "rate not valid!";
        playingReign.setFearRate(rate);
        return "fear rate set!";
    }
}
