package controller;

import model.*;
import model.buildings.Building;
import Enum.*;
import model.buildings.GateHouse;
import model.people.MilitaryUnit;

import java.util.regex.*;

public class BuildingController extends GameController{
    private final Building selectedBuilding;

    public BuildingController(Game game) {
        super(game);
        this.selectedBuilding = game.getSelectedBuilding();
    }


    public String showHitPoint() {
        return "left hitPoint of this building is " + selectedBuilding.getHp();
    }
    public String createUnit(Matcher matcher) {
        int count = Integer.parseInt(matcher.group("count"));
        UnitType unitType = UnitType.getUnitTypeByName(matcher.group("type"));
        if(unitType == null) return "wrong unit type";
        if(count <= 0) return "invalid count of troops";
        if(playingReign.getGold() < unitType.cost * count) return "you don't have enough gold";
        if(playingReign.getResourceAmount(unitType.resourceToBuild) < count)
            return "you don't have enough" + unitType.resourceToBuild.name() + "to build this unit";
        if(!unitType.buildingProducedIn.equals(selectedBuilding.buildingType))
            return "you can't build " + unitType.name() + "s in a" + selectedBuilding.buildingType.getName();
        if(playingReign.getUnemployedPopulation() < count) return "you don't have enough unemployed population";
        MilitaryUnit unit = new MilitaryUnit(unitType , selectedBuilding.getOwner(),
                selectedBuilding.getBlock(), count);
        playingReign.changeUnemployedPopulation(-count);
        playingReign.spendGold(count * unitType.cost);
        playingReign.changeResourceAmount(unitType.resourceToBuild, count);
        playingReign.getMilitaryUnits().add(unit);
        game.getAllOfTheUnits().add(unit);
        return "create unit successful";
    }
    public String repair() {
        if(selectedBuilding.getHp() == selectedBuilding.buildingType.hp) return "this building is already repaired";
        Resource resource = selectedBuilding.buildingType.resourceToBuild;
        if(resource != null &&
                playingReign.getResourceAmount(resource) < selectedBuilding.buildingType.resourceAmount)
            return "you don't have enough resources to repair this building";
        if(game.getMap().findUnitsOfOpponent(selectedBuilding.getBlock() , 3 ,  selectedBuilding.getOwner()))
            return "repair failed, there are units belonging to your opponent near this building";
        selectedBuilding.setHp(selectedBuilding.buildingType.hp);
        if(resource != null) selectedBuilding.getOwner().changeResourceAmount(resource, selectedBuilding.buildingType.resourceAmount);
        return "building repaired successfully";
    }

    public String changeTaxRate(int rate) {
        if(!(selectedBuilding instanceof GateHouse)) return "you can't change the tax rate with this building";
        if(rate <= 0) return "change tax failed: rate amount not correct";
        selectedBuilding.getOwner().setTaxRate(rate);
        return "set tax rate successful"; // todo check
    }
    public String ChangeFoodDistribution (int rate) {
        if(!(selectedBuilding instanceof GateHouse)) return "you can't change the food rate with this building";
        if(rate <= 0) return "change food failed: rate amount not correct";
        selectedBuilding.getOwner().setFoodRate(rate);
        return "set food rate successful";
    }
    public void deleteSelectedBuilding() {
        game.setSelectedBuilding(null);
    }
}
