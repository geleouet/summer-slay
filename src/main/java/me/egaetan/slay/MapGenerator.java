package me.egaetan.slay;

import java.util.*;

public class MapGenerator {

    enum FloorRoomType {
        // https://unicode-table.com/en/emoji/
        REGULAR(" ⚔ "),
        TREASURE(/*"\uD83D\uDC51 "*/" \uD83C\uDF81"),
        SHOP(" \uD83D\uDCB0"),
        REST(" \uD83D\uDCA4"),
        ELITE(" \uD83D\uDC80"),
        EVENT(" \uD83D\uDCDC"),
        BOSS(/*" \uD83D\uDD25"*/ " \uD83C\uDFC6"),
        ;

        final String shortName;

        FloorRoomType(String shortName) {
            this.shortName = shortName;
        }

    }

    static class FloorRoom {
        FloorRoomType type = FloorRoomType.REGULAR;
        private Floor floor;
        private int index;

        public FloorRoom(Floor floor, int index) {
            this.floor = floor;
            this.index = index;
        }
    }

    static class Floor {
        Set<Integer> path = new HashSet<>();
        Map<Integer, List<Integer>> linkTo = new HashMap<>();
        Map<Integer, List<Integer>> linkFrom = new HashMap<>();
        FloorRoom rooms[];
        private int index;

        public Floor(int index, int maxByFloor) {
            this.index = index;
            rooms = new FloorRoom[maxByFloor + 1];
            for (int i = 0; i <= maxByFloor; i++) {
                rooms[i] = new FloorRoom(this, i);
            }
        }
    }

    public static void main(String[] args) {
        Random r = new Random(44864643l);

        for (int i = 0; i < 1; i++) {
            List<Floor> floors = generateFloors(r, 6);
            debug(floors);
            System.out.println("_______________________________________________\n");
        }
    }

    private static void debug(List<Floor> floors) {
        for (int j = 0; j < floors.size(); j++) {
            Floor f = floors.get(j);
            for (int i = 0; i <= f.rooms.length; i++) {
                if (f.path.contains(i)) {
                    //Visited, highlight
                    //if (j == 7 && i == 5)
                    //System.out.print(" \033[1mX\033[0m  ");
                    //else if (j > 7 && i == 5)
                    //System.out.print(" \033[3mX\033[0m  ");
                    //else if (j >= 7)
                    //    System.out.print(" .  "/*" \033[9m✔\033[0m  "*/);
                    //else
                    System.out.print("" + f.rooms[i].type.shortName + " ");
                } else {
                    System.out.print("    ");
                }
            }
            System.out.println();

            // Options
            for (int i = 0; i <= f.rooms.length; i++) {
                if (f.path.contains(i)) {
                    System.out.print(" " + i + "  ");
                } else {
                    System.out.print("    ");
                }
            }
            System.out.println();



            if (j < floors.size() - 1) {
                for (int i = 0; i <= f.rooms.length; i++) {
                    if (f.linkTo.getOrDefault(i, Collections.emptyList()).contains(i - 1)) {
                        System.out.print("/");
                    } else {
                        System.out.print(" ");
                    }
                    if (f.linkTo.getOrDefault(i, Collections.emptyList()).contains(i)) {
                        System.out.print("|");
                    } else {
                        System.out.print(" ");
                    }
                    if (f.linkTo.getOrDefault(i, Collections.emptyList()).contains(i + 1)) {
                        System.out.print("\\");
                    } else {
                        System.out.print(" ");
                    }
                    System.out.print(" ");
                }
                System.out.println("");

                Floor g = floors.get(j + 1);
                for (int i = 0; i <= f.rooms.length; i++) {
                    if (g.linkFrom.getOrDefault(i, Collections.emptyList()).contains(i - 1)) {
                        System.out.print("\\");
                    } else {
                        System.out.print(" ");
                    }
                    if (g.linkFrom.getOrDefault(i, Collections.emptyList()).contains(i)) {
                        System.out.print("|");
                    } else {
                        System.out.print(" ");
                    }
                    if (g.linkFrom.getOrDefault(i, Collections.emptyList()).contains(i + 1)) {
                        System.out.print("/");
                    } else {
                        System.out.print(" ");
                    }
                    System.out.print(" ");
                }
                System.out.println("");
            }

        }
    }

    private static List<Floor> generateFloors(Random r, int maxByFloor) {
        int nPaths = 6;
        int nFloors = 15;

        List<Floor> floors = new ArrayList<>();
        Floor entry = new Floor(0, maxByFloor);
        floors.add(entry);
        for (int i = 0; i < nFloors; i++) {
            floors.add(new Floor(i+1, maxByFloor));
        }

        int entryPoint = r.nextInt(maxByFloor + 1);
        entry.path.add(entryPoint);
        for (int j = 0; j < nPaths; j++) {
            int prev = entryPoint;
            for (int i = 1; i <= nFloors; i++) {
                boolean crossing = true;
                int next = prev;
                while (crossing) {
                    int diff = r.nextInt(3) - 1;
                    next = prev + diff;

                    next = next < 0 ? 0 : next;
                    next = next > maxByFloor ? maxByFloor : next;

                    // check croisement
                    crossing = false;
                    for (Map.Entry<Integer, List<Integer>> e : floors.get(i - 1).linkTo.entrySet()) {
                        if (e.getKey() < prev && e.getValue().stream().max(Comparator.naturalOrder()).get() > next) {
                            crossing = true;
                        }
                        if (e.getKey() > prev && e.getValue().stream().min(Comparator.naturalOrder()).get() < next) {
                            crossing = true;
                        }
                    }
                }

                floors.get(i - 1).linkTo.computeIfAbsent(prev, __ -> new ArrayList<>()).add(next);
                floors.get(i).path.add(next);
                floors.get(i).linkFrom.computeIfAbsent(next, __ -> new ArrayList<>()).add(prev);
                prev = next;

            }
        }

        List<FloorRoom> rooms = new ArrayList<>();
        for (Floor f : floors) {
            for (int i = 0; i <= maxByFloor; i++) {
                if (f.path.contains(i)) {
                    rooms.add(f.rooms[i]);
                }
            }
        }

        for (int i = 0; i <= maxByFloor; i++) {
            floors.get(6).rooms[i].type = FloorRoomType.TREASURE;
            floors.get(1).rooms[i].type = FloorRoomType.REST;
            floors.get(0).rooms[i].type = FloorRoomType.BOSS;
            rooms.remove(floors.get(6).rooms[i]);
            rooms.remove(floors.get(1).rooms[i]);
            rooms.remove(floors.get(0).rooms[i]);
            rooms.remove(floors.get(15).rooms[i]);
        }

        Collections.shuffle(rooms, r);

        List<FloorRoomType> pool  = new ArrayList<>();
        int nbShops = (int) (rooms.size() * 0.06 + 0.5);
        for (int i = 0; i < nbShops; i++) {
            pool.add(FloorRoomType.SHOP);
        }
        int nbRest = (int) (rooms.size() * 0.12 + 0.5);
        for (int i = 0; i < nbRest; i++) {
            pool.add(FloorRoomType.REST);
        }
        int nbEvent = (int) (rooms.size() * 0.22 + 0.5);
        for (int i = 0; i < nbEvent; i++) {
            pool.add(FloorRoomType.EVENT);
        }
        int nbElite = (int) (rooms.size() * 0.13 + 0.5);
        for (int i = 0; i < nbElite; i++) {
            pool.add(FloorRoomType.ELITE);
        }

        Collections.shuffle(pool, r);

        Set<Integer> consumed = new HashSet<>();
        for (int i = 0; i < rooms.size(); i++) {
            FloorRoom room = rooms.get(i);
            Set<FloorRoomType> forbidden = new HashSet<>();

            // parents
            List<Integer> previouses = room.floor.linkFrom.get(room.index);
            {
                Floor prev = floors.get(room.floor.index - 1);
                for (int p : previouses) {
                    if (
                            prev.rooms[p].type == FloorRoomType.ELITE ||
                            prev.rooms[p].type == FloorRoomType.SHOP ||
                            prev.rooms[p].type == FloorRoomType.REST ||
                            prev.rooms[p].type == FloorRoomType.TREASURE
                    )
                    forbidden.add(prev.rooms[p].type);
                }
            }

            // childs
            List<Integer> childs = room.floor.linkTo.get(room.index);
            {
                Floor prev = floors.get(room.floor.index + 1);
                for (int p : childs) {
                    if (
                            prev.rooms[p].type == FloorRoomType.ELITE ||
                                    prev.rooms[p].type == FloorRoomType.SHOP ||
                                    prev.rooms[p].type == FloorRoomType.REST ||
                                    prev.rooms[p].type == FloorRoomType.TREASURE
                    )
                        forbidden.add(prev.rooms[p].type);
                }
            }

            // sisters
            List<Integer> nexts = room.floor.linkFrom.getOrDefault(room.index, Collections.emptyList());
            {
                Floor next = floors.get(room.floor.index - 1);
                for (int n : nexts) {
                    for (int k : next.linkTo.getOrDefault(n, Collections.emptyList())) {
                        forbidden.add(room.floor.rooms[k].type);
                    }
                }
            }


            for (int k = 0; k < pool.size(); k++) {
                FloorRoomType prop = pool.get(k);
                if (consumed.contains(k)) {
                    continue;
                }
                if (forbidden.contains(prop)) {
                    continue;
                }
                if (room.floor.index >= 11 && prop.equals(FloorRoomType.ELITE)) {
                    continue;
                }
                if (room.floor.index >= 11 && prop.equals(FloorRoomType.REST)) {
                    continue;
                }
                if (room.floor.index == 2 && prop.equals(FloorRoomType.REST)) {
                    continue;
                }
                consumed.add(k);
                room.type = prop;
                break;
            }
        }
        return floors;
    }

}
