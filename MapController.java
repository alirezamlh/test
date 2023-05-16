package controller;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import model.Block;
import model.Map;
import Enum.*;
import model.Reign;
import model.User;
import model.buildings.Building;
import model.people.MilitaryUnit;


import java.io.*;
import java.util.HashMap;
import java.util.regex.*;

public class MapController {
    //private Game game;
    private final Reign reignPlaying;
    private final Map map;
    private int x;
    private int y;
    public boolean isInTheGame;

    public MapController(Map map, boolean isInTheGame, Reign playing) {
        this.map = map;
        this.isInTheGame = isInTheGame;
        reignPlaying = playing;
    }

    public String showMap(Matcher matcher) {
        this.x = Integer.parseInt(matcher.group("x"));
        this.y = Integer.parseInt(matcher.group("y"));
        if (x > map.dimensions || y > map.dimensions) return "not in the map";
        return printMap(x, y);
    }

    public String printMap(int i, int j) {
        int underLimitX = i - 5;
        int underLimitY = j - 5;
        int upperLimitX = i + 5;
        int upperLimitY = j + 5;

        if (i < 5) {
            underLimitX = 0;
        }
        if (j < 5) {
            underLimitY = 0;
        }
        if (upperLimitX > map.dimensions) {
            upperLimitX = map.dimensions;
        }
        if (upperLimitY > map.dimensions) {
            upperLimitY = map.dimensions;
        }
        for (int x = underLimitX; x < upperLimitX; x++) {
            System.out.print("|");
            for (int y = underLimitY; y < upperLimitY; y++) {
                Block block = map.getBlockByLocation(x, y);
                if (block.getBuilding() != null) {
                    System.out.print(FieldType.getFieldTypeColor(block)+"#"+FieldType.ANSI_WHITE_BACKGROUND);
                    continue;
                } else if (block.getFieldType().isAquatic) {
                    System.out.print(FieldType.getFieldTypeColor(block)+"~"+FieldType.ANSI_WHITE_BACKGROUND);
                } else if (!block.isOccupied()) {
                    System.out.print(FieldType.getFieldTypeColor(block)+"X"+FieldType.ANSI_WHITE_BACKGROUND);
                    continue;
                } else if (map.isABase(x, y)) {
                    System.out.print(FieldType.getFieldTypeColor(block)+"@"+FieldType.ANSI_WHITE_BACKGROUND);
                    continue;
                } else if (block.getFieldType().isSuitableForBuilding) {
                    System.out.print(FieldType.getFieldTypeColor(block)+"^"+FieldType.ANSI_WHITE_BACKGROUND);
                } else
                    System.out.print(FieldType.getFieldTypeColor(block)+"+"+FieldType.ANSI_WHITE_BACKGROUND);
            }
            System.out.println("|");
        }

        return "-map|shown-";
    }

    public String moveMap(Matcher matcher) {
        HashMap<Direction, Integer> move = new HashMap<>();
        move.put(Direction.up, Integer.parseInt(matcher.group("up")));
        move.put(Direction.down, Integer.parseInt(matcher.group("down")));
        move.put(Direction.left, Integer.parseInt(matcher.group("left")));
        move.put(Direction.right, Integer.parseInt(matcher.group("right")));
        for (Direction direction : Direction.values()) {
            this.x += move.get(direction) * direction.xChange;
            this.y += move.get(direction) * direction.yChange;
        }
        return printMap(x, y);
    }

    public String showDetail(Matcher matcher) {
        this.x = Integer.parseInt(matcher.group("x"));
        this.y = Integer.parseInt(matcher.group("y"));
        if (!areCoordinatesValid(x, y))
            return "location is not valid!";
        return map.getBlockByLocation(x, y).BlockInfo(true);
    }

    public String setNewBase(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if (!areCoordinatesValid(x, y))
            return "location is not valid!";
        if (map.getBlockByLocation(x, y) == null) return ResponseToUser.INDEX.response;
        if (map.isABase(x, y)) return "this block already is a base";
        if (map.getBlockByLocation(x, y).isOccupied()) return "this block is occupied";
        if (map.getBaseBlocks().size() == 8) return "you have 8 bases you can't add more";
        if (map.getBlockByLocation(x, y).isOccupied())
            return "not good location for base!";
        map.getBaseBlocks().add(map.getBlockByLocation(x, y));
        return "base was added successfully";
    }

    public String removeBase(Matcher matcher) {
        if (!map.isABase(x, y)) return "there is no base in this block";
        if (isInTheGame) {
            return "wtf dude,you will be literally god with this option while this is my swamp and i'm drowsy...zzz";
        }
        map.getBaseBlocks().remove(map.getBlockByLocation(x, y));
        return "the base was successfully removed";
    }

    public String setTextureOfBlock(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if (!areCoordinatesValid(x, y))
            return "no valid point!";
        FieldType fieldType = FieldType.getFieldType(matcher.group("type"));
        if (fieldType == null)
            return "no valid field type!";
        if (isInTheGame) return "you can't change the texture of the blocks in the game";
        if (map.isABase(x, y)) return "there is a base here, you can't change the texture";
        map.getBlockByLocation(x, y).setFieldType(fieldType);
        return "the texture is now set";
    }

    public String setTextureOfArea(Matcher matcher) {
        FieldType fieldType = FieldType.getFieldType(matcher.group("type"));
        int x2 = Integer.parseInt(matcher.group("x2"));
        int x1 = Integer.parseInt(matcher.group("x1"));
        int y1 = Integer.parseInt(matcher.group("y1"));
        int y2 = Integer.parseInt(matcher.group("y2"));
        if (isInTheGame) return "you can't change the texture of the blocks in the game";
        for (int i = x1; i <= x2; i++) {
            for (int j = y1; j <= y2; j++) {
                if (map.getBlockByLocation(x, y) != null && map.getBlockByLocation(x, y).getBuilding() == null)
                    map.getBlockByLocation(i, j).setFieldType(fieldType);
            }
        }
        return "the textures are successfully set";
    }

    public String dropBuilding(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if (!areCoordinatesValid(x, y))
            return "your point is not valid!";
        Block block = map.getBlockByLocation(x, y);
        if (block.getBuilding() == null)
            return "there is already building in that point!";
        if (block.isOccupied())
            return "you can't build anything here!";
        BuildingType buildingType = BuildingType.getBuildingTypeByName(matcher.group("type"));
        if (buildingType == null)
            return "your chosen building is not valid!";
        Building buildingToAdd = new Building(buildingType, reignPlaying, block);

        if (reignPlaying == null) {
            block.setBuilding(buildingToAdd);
        } else {
            block.setBuilding(buildingToAdd);
            reignPlaying.addBuilding(buildingToAdd);
        }
        return "building dropped with success!";
    }

    public String dropUnit(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        UnitType unitType = UnitType.getUnitTypeByName(matcher.group("type"));
        int amount = Integer.parseInt(matcher.group("amount"));
        if (!areCoordinatesValid(x, y))
            return "there is no valid point!";
        else if (unitType == null)
            return "there is no valid unit Type";
        else if (amount < 1)
            return "no valid amount!";
        else if (!isInTheGame)
            return "You are not playing!";
        else {
            MilitaryUnit militaryUnit = new MilitaryUnit(unitType, reignPlaying, map.getBlockByLocation(x, y), amount);
            reignPlaying.getMilitaryUnits().add(militaryUnit);
            map.getBlockByLocation(x, y).addUnit(militaryUnit);
            return "Units were deployed(cheater00)!";
        }
    }


    public String dropRock(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if(!areCoordinatesValid(x,y))
            return "location is not valid!";
        Direction direction = Direction.getDirectionByName(matcher.group("direction"));
        if(direction == null)
            return "direction not valid!";
        Block block = map.getBlockByLocation(x,y);
        if((block.getBuilding() != null) && (block.getMilitaryUnits() != null))
            return "there are already some buildings and units in block!";
        else{
            block.setFieldType(FieldType.Rock);
            return "an step toward new stone age!";
        }
    }

    public String dropTree(Matcher matcher) {
        Tree tree = Tree.getTreeByName(matcher.group("tree"));
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if(!areCoordinatesValid(x,y))
            return "location is not valid!";
        if(tree == null)
            return "No such tree exists!";
        Block block = map.getBlockByLocation(x, y);
        if(block == null) return "index out of bounds";
        if(block.isOccupied()) return ResponseToUser.OCCUPIED.response;
        block.setTree(tree);
        return "tree was dropped successfully";
    }

    public String clearBlock(Matcher matcher) {
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        if(!areCoordinatesValid(x,y))
            return "location is not valid!";
        if(!isInTheGame) {
            map.getBlocks().remove(map.getBlockByLocation(x, y));
            map.getBlocks().add(new Block(x ,y , FieldType.Ground));
        }
        else {
            map.getBlockByLocation(x, y).clearBlock(reignPlaying);
        }
        return "block was cleared successfully";
    }

    private boolean areCoordinatesValid(int x, int y) {
        if(x > 0 && x < map.dimensions && y > 0 && y < map.dimensions)
            return true;
        return false;
    }

    public int getSizeOfMap() {
        return map.dimensions;
    }

    public FieldType getTextureOfBlock(int x, int y) {
        return map.getBlockByLocation(x, y).getFieldType();
    }
    public static void saveTheMaps(){
        Gson gson = new Gson();
        String json = gson.toJson(Map.getTemplateMaps());
        try {
            FileWriter myWriter = new FileWriter("dataBaseMap.json");
            myWriter.write(json);
            myWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public static void loadTheMaps() {
        Reader reader;
        try {
            reader = new FileReader("dataBaseMap.json");
        } catch (FileNotFoundException e) {
            return;
        }
        Gson gson = new Gson();
        JsonArray jsonArray = gson.fromJson(reader, JsonArray.class);
        for (JsonElement jsonElement : jsonArray)
            Map.getTemplateMaps().add(gson.fromJson(jsonElement, Map.class));
    }

    public Map getMap() {
        return map;
    }
}
