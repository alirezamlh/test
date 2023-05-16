package controller;

import Enum.*;
import model.*;
import model.buildings.Building;
import model.buildings.Wall;
import model.people.*;
import model.structures.*;
import model.people.MilitaryUnit;
import model.buildings.Tower;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

public class UnitController extends GameController{
    private MilitaryUnit selectedUnit;
    private boolean isMoving;
    public UnitController(Game game) {
        super(game);
        selectedUnit = game.getSelectedUnit();
    }

    public String moveUnitCommand(int x, int y) {
        if (!areCoordinatesCorrect(x, y)) return ResponseToUser.COORDINATES_NOT_CORRECT.response;
        if (selectedUnit instanceof Engineer) {
            if(((Engineer)selectedUnit).hasStructure())
                return "you can't move an engineering unit inside a structure\ntry moving the structure itself";
        }
        if (x == selectedUnit.getBlock().x && y == selectedUnit.getBlock().y) return "troops are already in this block";
        String destMessage;
        if (!(destMessage = checkTheDestination(x, y)).equals("correct")) return destMessage;
        selectedUnit.setDestination(map.getBlockByLocation(x, y));
        String result = move(selectedUnit);
        if(result.equals("no path")) return "there is no path to this block";
        if(result.equals("killed")) return "the Military unit was killed by a trap";
        if(result.equals("arrived")) return "move unit successful. the unit is in the dest block";
        return "the unit is moving toward the destination block";
    }

    public String patrolUnit(int x1, int x2, int y1, int y2) {
        if(!areCoordinatesCorrect(x1, y2) || !areCoordinatesCorrect(x2, y2))
            return ResponseToUser.COORDINATES_NOT_CORRECT.response;
        if(!checkTheDestination(x1, y1).equals("correct") || !checkTheDestination(x2, y2).equals("correct"))
            return "you can't go into one of these blocks";
        //todo check if the unit can patrol
        if (findAPath(map.getBlockByLocation(x1, y1), map.getBlockByLocation(x2, y2)) == null)
            return "there is no path to connect these blocks";
        if (findAPath(selectedUnit.getBlock(), map.getBlockByLocation(x1, y1)) == null)
            return "there is no path to go to the first block";
        selectedUnit.setDestination(map.getBlockByLocation(x1, y1));
        selectedUnit.setSecondDestBlock(map.getBlockByLocation(x2, y2));
        String result = patrol(selectedUnit);
        if(result.equals("killed")) return "unit was killed by a trap";
        return "the patrol has begun";
    }
    public String patrol(MilitaryUnit unit) {
        String result = move(unit);
        if(result.equals("no path") || result.equals("killed")) unit.stopMoving();
        if(result.equals("arrived")) unit.changeDestForPatrol();
        return result;
    }
    public ArrayList<Block> findAPath(Block start, Block dest) {
        ArrayList<Block> closed = new ArrayList<>();
        TreeMap<Integer, Block> openList = new TreeMap<>();
        HashMap<Block, Integer> gOfBlocks = new HashMap<>();
        HashMap<Block, Block> father = new HashMap<>();

        father.put(start, start);
        openList.put(0, start);
        while(true) {
            Block block = openList.firstEntry().getValue();
            openList.remove(openList.firstEntry().getKey(), openList.firstEntry().getValue());
            closed.add(block);
            Block nextBlock;
            for (Direction value : Direction.values()) {
                nextBlock = map.getNeighborBlock(block, value);
                if(nextBlock == null) continue;
                if (!nextBlock.isPassable()) continue;
                if(closed.contains(nextBlock)) continue;
                if (nextBlock.equals(dest)) {
                    father.put(nextBlock, block);
                    return arrayListMaker(father, dest);
                }
                int g = gOfBlocks.get(block) + 1;
                int h = getMaxDistance(nextBlock, dest);
                // todo check the previous f
                openList.put(h + g, nextBlock);
                if(gOfBlocks.containsKey(nextBlock)) gOfBlocks.replace(nextBlock, g);
                else gOfBlocks.put(nextBlock, g);
                father.put(nextBlock, block);
            }
            if(openList.size() == 0) return null;
        }

    }

    public String move(MilitaryUnit unit) {
        ArrayList<Block> path = findAPath(unit.getBlock(), unit.getDestBlock());
        if(path == null) return "no path";
        for(int i = 1; i<= unit.unitType.speed; i++) {
            moveTo(unit, path.get(i));
            if(path.get(i).isATrapFor(unit)){
                game.removeUnitIfKilled(unit);
                return "killed";
            }
            if(path.get(i).equals(unit.getDestBlock())) return "arrived";
        }
        return "still moving";
    }
    private void moveTo(MilitaryUnit unit, Block dest) {
        unit.getBlock().removeUnit(unit);
        unit.setBlock(dest);
        dest.getMilitaryUnits().add(unit);
    }

    public ArrayList<Block> arrayListMaker(HashMap<Block, Block> father, Block dest) {
        ArrayList<Block> output = new ArrayList<>();
        output.add(dest);
        Block currectBlock = dest;
        Block fatherB;
        while (true) {
            fatherB = father.get(currectBlock);
            if(father.equals(currectBlock)) break;
            output.add(0, fatherB);
            currectBlock = fatherB;
        }
        return output;
    }

    public int getMaxDistance(Block current, Block dest) {
        int x = Math.abs(current.x - dest.x);
        int y = Math.abs(current.y - dest.y);
        if(x > y) return x;
        return y;
    }
    public String checkTheDestination(int x, int y) {
        if(!map.getBlockByLocation(x, y).getFieldType().canTroopPass) return "you can't move this unit to a "
                + map.getBlockByLocation(x, y).getFieldType().getName();
        if(!map.getBlockByLocation(x, y).getBuilding().buildingType.isPassableForTroop)
            return "units can't go to this block because of the building in the block";
        return "correct";
    }



    public String setUnitState(String state) {
        UnitState unitState = UnitState.getUnitState(state);
        if(unitState == null) return "please set a valid state for your selected unit";
        if(unitState.equals(selectedUnit.getUnitState())) return "the unit is already in this state";
        selectedUnit.setUnitState(unitState);
        return "the unit state is selected to" + state;
    }

    public String getUnitState() {
        return "the selected unit state is: " + game.getSelectedSingleUnit().getUnitState();
    }

    public String attackEnemy(int x , int y) {
        Block block = map.getBlockByLocation(x, y);
        if (areCoordinatesCorrect(x, y)) return ResponseToUser.COORDINATES_NOT_CORRECT.response;
        if (!selectedUnit.isBlockInRange(block)) return "you can't attack this block because its not in the units range";
        if (block.findOpponentInBlock(playingReign)) return "there is no enemy in this block";
        return attackBlock(selectedUnit, block, true);

    }
    public String attackBlock(MilitaryUnit attacker , Block block, boolean isInTurn) {
        if(!attacker.isBlockInRange(block)) return "not in range";
        if(block.hasABuilding()){
            Building building = block.getBuilding();
            if(!building.getOwner().equals(attacker.getOwner())) {
                building.getDamaged(selectedUnit.getDamage());
                if (building.getHp() <= 0) {
                   String result = super.removeBuilding(building);
                   if(result.equals("endGame")) return "endGame";
                }
            }
        }
        for (MilitaryUnit unit : block.getMilitaryUnits()) {
            boolean isAttackerKilled = militaryAttack(attacker, unit, isInTurn);
            if(isAttackerKilled) return "attacker killed";
        }
        return "attack done";
    }
    public Boolean militaryAttack(MilitaryUnit first , MilitaryUnit second, boolean isInTurn) {
        if (first.getOwner().equals(second.getOwner())) return false;
        second.getDamaged(first.getDamage());
        if (isInTurn) {
            if (second.isBlockInRange(first.getBlock())) {
                first.getDamaged(second.getDamage());
            }
        }
        game.removeUnitIfKilled(second);
        if (first.getHp() <= 0) {
            game.removeUnitIfKilled(first);
            return true;
        }
        return false;
    }



    public String airAttack(int x, int y) {
        if(!areCoordinatesCorrect(x,y))
            return "location is not valid!";
        else if(game.getSelectedSingleUnit().getRange() < 2){
            return "selected unit can't attack ranged";
        }
        else if(getDistance(game.getSelectedSingleUnit().getBlock(),game.getMap().getBlockByLocation(x,y)) >
                game.getSelectedSingleUnit().getRange()){
            return "aim is out of board!";
        }
        else if(game.getMap().getBlockByLocation(x,y).getMilitaryUnits() == null){
            return "there is no enemy in destination";
        }
        else{
            int amountOfDamage = game.getSelectedSingleUnit().getDamage()*game.getSelectedSingleUnit().getNumber();
            game.getMap().getBlockByLocation(x,y).getMilitaryUnits().get(0).getDamaged(amountOfDamage);
            collectingGarbageUnits();
            return "attack successfully!";
        }

    }

    public String pourOil(String dir) {
        if(!selectedUnit.unitType.equals(UnitType.ENGINEER)) return "only an engineer can pour oil";
        Direction direction = Direction.getDirectionByName(dir);
        if(direction == null) return "you have entered the wrong direction";
        if(!((Engineer) selectedUnit).hasOilToPour()) return "the Engineer doesn't have oil to pour";
        Block block = selectedUnit.getBlock();
        for(int i = 0; i < 3; i++) {
            block= map.getNeighborBlock(block, direction);
            for (MilitaryUnit unit : block.getMilitaryUnits()) {
                unit.getDamaged(10);
                game.removeUnitIfKilled(unit);
            }
        }
        return "pour oil successful";
    }
    public String digTunnel(int x , int y) {
        if(!areCoordinatesCorrect(x,y))
            return "location is not valid!";
        else if(getDistance(map.getBlockByLocation(x,y),game.getSelectedSingleUnit().getBlock()) >= 2)
            return "your are too far away,try to move toward the aim!";
        Block block = map.getBlockByLocation(x,y);
        if(!(game.getSelectedSingleUnit() instanceof Tunneler))
            return "you can't dig tunnel without tunnelers!";
        else if(!(block.getBuilding()==null) || !(block.getBuilding() instanceof Wall) ||
                !(block.getBuilding().buildingType.equals(BuildingType
                .DEFENCE_TURRET))){
            return "you can't dig tunnel here!";
        }
        else{
            block.getMilitaryUnits().clear();
            block.addUnit(game.getSelectedSingleUnit());
            game.getSelectedSingleUnit().setBlock(block);
            block.removeBuilding();
            return "tunneling was amazing!";
        }
    }
    public int getDistance(Block blockIN,Block blockOUT){
        int xD = blockIN.getX() - blockOUT.getX();
        int yD = blockIN.getY() - blockOUT.getY();
        double out = xD*xD + yD*yD;
        out = Math.sqrt(out);
        int outage = (int) Math.floor(out);
        return outage;
    }
    public String buildStructure(String structureType) {
        StructuresType type = StructuresType.getType(structureType);
        if (!(selectedUnit instanceof Engineer))
            return "You must choose bunch of engineers!";
        if (playingReign.getResourceAmount(type.resource) < type.getAmountOfMaterial())
            return "You don't have enough material to build this structure";
        if (selectedUnit.getNumber() < type.getAmountOfEngineer()) return "You don't have enough engineers!";
        if(selectedUnit.getBlock().canPutStructure(type)) return "you can't put a " + type.name() + "in this block";
        if(type.equals(StructuresType.LADDER)) {
            game.getSelectedSingleUnit().setNumber((game.getSelectedSingleUnit()).getNumber());
            LadderMen ladderMen = new LadderMen(UnitType.LADDERMAN,game.getPlayingReign(),
                    game.getSelectedSingleUnit().getBlock(),1);
            ladderMen.getBlock().addUnit(ladderMen);
            ladderMen.getOwner().getMilitaryUnits().add(ladderMen);
            return "ladder was created successfully";
        } else if(type.equals(StructuresType.STAIRS)){
            if(!(game.getSelectedSingleUnit().getBlock().getBuilding() instanceof Wall
                    || game.getSelectedSingleUnit().getBlock().getBuilding() instanceof Tower)){
                return "You can't build stairs here!";
            }
        } else if(type.equals(StructuresType.MOVING_SHIELD)) {
        } else if(type.equals(StructuresType.WALL_BREAKER)) {
             if(!(game.getSelectedSingleUnit().getBlock().getBuilding()==null)) return "You can't build wall breaker here!";

        } else if(type.equals(StructuresType.SIEGE_TOWER)) {
        } else if(type.equals(StructuresType.CATAPULT)) {
            if(!(game.getSelectedSingleUnit().getBlock().getBuilding()==null)){
                return "You can't build catapult here!";
            }
        } else if(type.equals(StructuresType.TREBUCHET)) {
            if(!(game.getSelectedSingleUnit().getBlock().getBuilding()==null &&
                    !(game.getSelectedSingleUnit().getBlock().getBuilding() instanceof Tower))){
                return "You can't build trebuchet here!";
            }
        } else if(type.equals(StructuresType.FLAME_THROWER)) {
            if(!(game.getSelectedSingleUnit().getBlock().getBuilding()==null &&
                    !(game.getSelectedSingleUnit().getBlock().getBuilding() instanceof Tower))){
                return "You can't build ballista here!";
            }
        }
        selectedUnit.getBlock().addNewStructure(new Structure(type, playingReign, (Engineer) selectedUnit, selectedUnit.getBlock()));
        game.getPlayingReign().spendResources(type.resource, type.getAmountOfMaterial());
        return "structure was created successfully";
    }

    public String digMoat(int x, int y) {
        if(!areCoordinatesCorrect(x,y))
            return "location is not valid!";
        else if(getDistance(map.getBlockByLocation(x,y),game.getSelectedSingleUnit().getBlock()) >= 2)
            return "your are too far away,try to move toward the aim!";
        if(map.getBlockByLocation(x,y).getFieldType().equals(FieldType.moat)){
            return "its already digged up?";
        }
        else if(map.getBlockByLocation(x,y).getMilitaryUnits().size() > 0)
            return "There are some units overthere!";
        else if(map.getBlockByLocation(x,y).isOccupied()){
            return "there are some buildings on location.";
        }
        map.getBlockByLocation(x,y).setFieldType(FieldType.moat);
        return "moat was digged up!";
    }

    public String fillMoat(int x, int y) {
        if(!areCoordinatesCorrect(x,y))
            return "location is not valid!";
        else if(getDistance(map.getBlockByLocation(x,y),game.getSelectedSingleUnit().getBlock()) >= 2)
            return "your are too far away,try to move toward the aim!";
        if(!map.getBlockByLocation(x,y).getFieldType().equals(FieldType.moat)){
            return "what are you gonna fill?!";
        }
        map.getBlockByLocation(x,y).setFieldType(FieldType.Ground);
        return "moat was filled";
    }

    public String putOnLadder(int x, int y) {
        if(!areCoordinatesCorrect(x,y))
            return "location is not valid!";
        else if(getDistance(map.getBlockByLocation(x,y),game.getSelectedSingleUnit().getBlock()) >= 2)
            return "your are too far away,try to move toward the aim!";
        boolean hasLadder = false;
        for(Structure structure : map.getBlockByLocation(x,y).getStructures()){
            if(structure instanceof Ladder){
                hasLadder = true;
            }
        }
        if(hasLadder){
            return "there is some ladder on the wall!";
        }
        else if(!(game.getSelectedSingleUnit() instanceof LadderMen)){
            return "the chosen unit can't put ladder on the wall!";
        }
        else if(!(map.getBlockByLocation(x,y).getBuilding() instanceof Wall)){
            return "you can't lay the ladder on nothing or that thing!";
        }
        else{
            Ladder ladder = new Ladder(StructuresType.LADDER, playingReign ,(Engineer) selectedUnit, selectedUnit.getBlock());
            game.getSelectedSingleUnit().setNumber((game.getSelectedSingleUnit()).getNumber()+-1);
            map.getBlockByLocation(x,y).addNewStructure(ladder);
            return "ladder was put on with success!";
        }
    }

    public String pushOffLadder(int x , int y) {
        if(!areCoordinatesCorrect(x,y))
            return "location is not valid!";
        else if(getDistance(map.getBlockByLocation(x,y),game.getSelectedSingleUnit().getBlock()) >= 2)
            return "your are too far away,try to move toward the aim!";
        boolean hasLadder = false;
        Ladder ladderToRemove = null;
        for(Structure structure : map.getBlockByLocation(x,y).getStructures()){
            if(structure instanceof Ladder){
                hasLadder = true;
                ladderToRemove = (Ladder)structure;
            }
        }
        if(!hasLadder){
            return "there is no ladder on the wall!";
        }
        else{
            map.getBlockByLocation(x,y).removeStructure(ladderToRemove);
            return "ladder was successfully removed!";
        }
    }

    public String disbandUnit() {
        int numberOfSolider = game.getSelectedSingleUnit().getNumber();
        game.getSelectedSingleUnit().setNumber(0);
        game.getSelectedSingleUnit().getOwner().changeUnemployedPopulation(+numberOfSolider);
        game.setSelectedUnit(null);
        collectingGarbageUnits();
        return "the unit was disbanded!";
    }

    public String moveStructure(int x, int y) {
        if(!areCoordinatesCorrect(x, y)) return ResponseToUser.COORDINATES_NOT_CORRECT.response;
        if(!selectedUnit.unitType.equals(UnitType.ENGINEER)) return "you should select an engineer unit to move a structure";
        Engineer engineer = (Engineer) selectedUnit;
        if(!engineer.hasStructure()) return "this unit has no structure to move";
        if(!map.getBlockByLocation(x, y).canPutStructure(engineer.getStructure().getType()))
            return "you can't move structure to this location";
        engineer.setDestination(map.getBlockByLocation(x, y));
        String result = move(engineer);
        if(result.equals("no path")) return "there is no path to this block";
        if(result.equals("killed")) return "the Military unit was killed by a trap";
        if(result.equals("arrived")) return "moving structure successful. the structure is in the dest block";
        return "the unit is moving toward the destination block";
    }


    public void deleteSelectedUnits() {
        game.setSelectedUnit(null);
    }
}
