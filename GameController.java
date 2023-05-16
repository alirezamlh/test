package controller;

import model.*;
import model.buildings.*;

import java.util.ArrayList;
import java.util.regex.Matcher;
import Enum.*;

import model.people.Engineer;
import model.people.MilitaryUnit;
import view.GameMenu;

public class GameController {
    private UnitController unitController;
    protected Reign playingReign;
    protected Game game;
    protected Map map;

    public GameController(Game game) {
        this.game = game;
        this.map = game.getMap();
        this.playingReign = game.getPlayingReign();
        this.unitController = new UnitController(game);
    }

    public String addUser(Matcher matcher) {
        User user = User.getUserByUsername(matcher.group("username"));
        if(user == null) return "this user does not exist";
        if(game.isUserInTheGame(user)) return "this user is already in the game";
        game.addReign(user);
        return "user added successfully";
    }

    public String dropBuilding(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if(!areCoordinatesCorrect(x, y)) return "coordinates are not correct";
        BuildingType type = BuildingType.getBuildingTypeByName(matcher.group("type"));
        if(type == null) return "wrong building type";
        Block block = map.getBlockByLocation(x, y);
        if (!block.getFieldType().isSuitableForBuilding)
            return "the filed type is not suitable to drop building";
        if(map.getBlockByLocation(x, y).getBuilding() != null) return "there is already a building in this block";
        if(map.getBlockByLocation(x, y).getStructures().size() > 0) {
            return "drop building failed: there are some structures in this block";
        }
        if(type.equals(BuildingType.BASE)) return "you can't build a new base inside a game";
        if(type.equals(BuildingType.PITCH_RIG) && !block.getFieldType().equals(FieldType.plain))
            return "you should build the pitch rig on a plain";
        // todo check the side blocks for the same type for stoke pile and ?? ..
        if(type.goldCost > playingReign.getGold()) return "you do not have enough gold";
        if(playingReign.getResourceAmount(type.resourceToBuild) < type.resourceAmount)
            return "you do not have enough resources to build this building";
        playingReign.spendGold(type.goldCost);
        playingReign.changeResourceAmount(type.resourceToBuild, type.resourceAmount);

        return BuildTheBuilding(type, block);

    }
    public String BuildTheBuilding(BuildingType type, Block block) {
        Building building = null; //todo check for the buildings that need the same building around them
        if(type.checkForEquals(BuildingType.SHOP, BuildingType.BARRACK, BuildingType.MERCENARY_CAMP, BuildingType.ENGINEER_GUILD)) {
            building = new Building(type, playingReign, block);
        }
        else if(type.checkForEquals(BuildingType.SMALL_STONE_GATE, BuildingType.BIG_STONE_GATEHOUSE)) {
             building = new GateHouse(type, playingReign, block);
        } else if(type.checkForEquals(BuildingType.HOVEL)) {
            building = new House(type, playingReign, block);
        } else if(type.checkForEquals(BuildingType.MOVING_BRIDGE)) {
            building = new MovingBridge(type, playingReign, block);
        } else if(type.checkForEquals(BuildingType.LOOKOUT_TOWER, BuildingType.PERIMETER_TOWER, BuildingType.DEFENCE_TURRET)) {
            building = new Tower(type, playingReign, block);
        } else if(type.checkForEquals(BuildingType.SQUARE_TOWER, BuildingType.CIRCLE_TOWER)) {
            building = new Tower(type, playingReign, block);
        } else if(type.checkForEquals(BuildingType.ARMOURY)) {
            building = new StoreHouse(type, playingReign, block); // delete this class?
        }
        else if(type.checkForEquals(BuildingType.ARMOURER, BuildingType.MILL, BuildingType.BLACK_SMITH,
                BuildingType.FLETCHER, BuildingType.POLE_TURNER, BuildingType.HUNTING_GROUND, BuildingType.BREWERY)) {
            building = new Converter(type, playingReign, block, Resource.getEntry(type), Resource.getProduct(type));
        }
        else if(type.checkForEquals(BuildingType.INN)) {
            building = new Inn(type, playingReign, block); // delete???
        }
        else if(type.checkForEquals(BuildingType.PITCH_RIG)) {
            if(!block.getFieldType().equals(FieldType.plain)) return "you should drop a pitch rig on a plain";
            building = new Producer(type, playingReign, block, Resource.TAR);
        } else if(type.checkForEquals(BuildingType.STONE_MINE)) {
            if(!block.getFieldType().equals(FieldType.Stone)) return "you should drop a stone mine on a Stone field";
            building = new Producer(type, playingReign, block , Resource.STONE);
        } else if(type.checkForEquals(BuildingType.IRON_MINE)) {
            if(!block.getFieldType().equals(FieldType.Iron)) return "you should drop an Iron Mine on an Iron field";
            building = new Producer(type, playingReign, block, Resource.IRON);
        }
        else if(type.checkForEquals(BuildingType.STOCK_PILE, BuildingType.FOOD_STOCK_PILE)) {
            if(!foundTheSameBuildingAround(block, type)) return "you should place a this building around a building of its type";
            building = new StoreHouse(type, playingReign, block);
        }
        else if(type.checkForEquals(BuildingType.CHURCH, BuildingType.CATHEDRAL)) {
            building = new Church(type, playingReign , block);
        }
        else if(type.checkForEquals(BuildingType.APPLE_GARDEN, BuildingType.WHEAT_FARM, BuildingType.BAKERY, BuildingType.DAIRY_FARM)) {
            building = new Producer(type, playingReign, block, Resource.getProduct(type));
        } else if(type.checkForEquals(BuildingType.CAGED_WAR_DOGS)) {
            building = new dogCage(type, playingReign , block);
        }
        game.addBuilding(building);


        return "drop building successful";
    }
    public String dropWall(Matcher matcher) {
        return null;
    }
    public String dropStairs(Matcher matcher) {
        return null;
    }
    public String selectBuilding(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if(!areCoordinatesCorrect(x, y)) return "coordinates are not correct";
        Building building = map.getBlockByLocation(x, y).getBuilding();
        if(building == null) return "there is no building in this block";
        if(!building.isOwnerCorrect(game.getPlayingReign())) return "the building is not yours";
        game.setSelectedBuilding(building);
        return "select building successful";
    }

    public String selectUnit(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if(!areCoordinatesCorrect(x , y)) return "the coordinates you have entered is not in the map";
        if(game.getUnitsOfBlock(x, y).size() == 0) return "there is no unit in this block";
        ArrayList<MilitaryUnit> units = game.getUnitsOfReignInBlock(playingReign , x , y);
        if(units.size() == 0) return "you don't have any unit in this block";
        if (units.size() > 1) {
            int unitNumber = GameMenu.askUserTheUnitToSelect(printUnits(units));
            if((units.get(unitNumber)) != null) game.setSelectedUnit(units.get(unitNumber));
            else return "you did not choose the right number of the unit";
        } else {
            game.setSelectedUnit(units.get(0));
        }
        return "select units successful!";
    }

    public String printUnits(ArrayList<MilitaryUnit> units) {
        String output = "choose one of the units below by writing the ordinal number of the unit";
        int i = 1;
        for (MilitaryUnit unit : units) {
            if(unit.getUnitType().equals(UnitType.ENGINEER)){
                Engineer engineer = (Engineer) unit;
                output += "\n" + i + ": unit " + unit.getUnitType();
                if(engineer.hasStructure()) output += "inside a " + engineer.getStructure().getType().name();
                if(engineer.hasOilToPour()) output += "with oil to pour";
            }
            output += "\n" + i + ": unit " + unit.getUnitType() + "with the number " + unit.getNumber()
                    + "and the state: " + unit.getUnitState();
            i++;
        }
        return output;
    }

    // drop unit chie aslan?????


    public String nextReign() {
        for (Building building : playingReign.getBuildings()) {
            building.nextTurn();
        }
        playingReign.distributeFood(Resource.APPLE, Resource.PROCESSED_MEAT, Resource.BREAD, Resource.CHEESE);
        playingReign.getTaxFromPeople();
        playingReign.calculatePopularityRate();
        playingReign = game.getNextReign();
        if(playingReign.equals(game.getReigns().get(0))) nextTurn();
        if(game.getReigns().size() == 1) return "endGame";
        return "turn switched.\nplayer " + playingReign.getNickName() + "is now playing!";
    }
    public void nextTurn() {
        game.oneTurnPassed();
        boolean opponentFound = false;
        for (MilitaryUnit unit : game.getAllOfTheUnits()) {
            ArrayList<Block> blocks = getBlocksAround(unit.getBlock(), unit.getUnitType().range);
            for (Block block : blocks) {
                String result = unitController.attackBlock(unit, block, false);
                if(block.findOpponentInBlock(playingReign)) opponentFound = true;
                if(result.equals("endGame")) return;
            }
            if(!opponentFound && !unit.isPatrolling() && !unit.isMoving()) {
                 Block block = findTheNearestOpponent(unit.getBlock(), unit.getUnitState().getRange());
                 if(block != null) {
                     unit.setDestination(block);
                     unitController.move(unit);
                 }
            }
        }
    }
    public Block findTheNearestOpponent(Block block, int range) {
        for(int i = 1;; i++) {
            Block block1 = getOpponentInRange(block, i);
            if(block1 != null) return block1;
            if(i == range) return null;
        }
    }
    public Block getOpponentInRange(Block block,  int range) {
        for(int i = -range; i <= range; i++) {
            for(int j = -range; j <= range; j++) {
                Block block1 = map.getBlockByLocation(block.x + i, block.y + j);
                if(block1.findOpponentInBlock(playingReign)) return block1;
            }
        }
        return null;
    }
    public ArrayList<Block> getBlocksAround (Block block , int range) {
        ArrayList<Block> blocksAround = new ArrayList<>();
        for(int i = -range; i <= range; i++) {
            for (int j = -range; j <= range; j++) {
                blocksAround.add(map.getBlockByLocation(block.x + i, block.y + j));
            }
        }
        return blocksAround;
    }
    public String removeBuilding(Building building) {
        if(building.getHp() > 0) return "";
        game.getAllTheBuildings().remove(building);
        building.getBlock().removeBuilding();
        building.getOwner().getBuildings().remove(building);
        if(building instanceof Base){
            endReign(building.getOwner());
            GameMenu.announceDeadReign(building.getOwner(), calculateAndGiveScore(building.getOwner()));
        }
        if(game.getReigns().size() == 1) return "endGame";
        return "";
    }
    public void endReign(Reign reign) {
        for (Building building : reign.getBuildings()) {
            building.getBlock().removeBuilding();
            game.getAllTheBuildings().remove(building);
        }
        for (MilitaryUnit unit : reign.getMilitaryUnits()) {
            unit.getBlock().removeUnit(unit);
            game.getAllOfTheUnits().remove(unit);
        }
        game.getReigns().remove(reign);
        if(game.getReigns().size() == 1) endGame();
        // structures?
    }
    public int calculateAndGiveScore(Reign reign) {
        int score = (int) ((game.getNumberOfPlayers() - game.getReigns().size()) * game.getTurnsPassed() * reign.getGold() * 100);
        if(game.getReigns().size() == 1) score *= 2;
        reign.getUser().addScore(score);
        return score;
    }
    public String endGame() {
        int score = calculateAndGiveScore(game.getReigns().get(0));
        return "the game is now over and the winner is: " + game.getReigns().get(0).getNickName() + " with the score of " + score ;
    }
    public String quitGame(Reign reign){
        endReign(reign);
        return "player " + reign.getNickName() + "has quit the game!";
    }
    public void collectingGarbageUnits(){
        for(MilitaryUnit militaryUnit:game.getAllOfTheUnits()){
            if(militaryUnit.getNumber() < 1){
                game.getAllOfTheUnits().remove(militaryUnit);
                militaryUnit.getOwner().getMilitaryUnits().remove(militaryUnit);
            }
        }
    }
    public String showTurnsPassed(){
        String output = game.getTurnsPassed() + "turns have passed up to now!";
        output += "\ndo not give up " + playingReign.getNickName() + ". we have a long way to go!";
        return output;
    }

    public boolean foundTheSameBuildingAround(Block block, BuildingType type) {
        for (Direction value : Direction.values()) {
            Block block1 = map.getNeighborBlock(block, value);
            if(block1.hasABuilding())
                if(block1.getBuilding().buildingType.equals(type)) return true;
        }
        return false;
    }

    public Game getGame () {
        return game;
    }
    public Reign getPlayingReign() {
        return playingReign;
    }
    public Map getMap() {
        return game.getMap();
    }
    public UnitController getUnitController() {
        return unitController;
    }
    public boolean areCoordinatesCorrect(int x , int y) {
        int dimension = game.getMap().dimensions;
        if(x <= dimension && x >= 1 && y <= dimension && y >= 1) return true;
        return false;
    }
}


