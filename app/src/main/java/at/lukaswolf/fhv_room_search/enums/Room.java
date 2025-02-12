package at.lukaswolf.fhv_room_search.enums;

import lombok.Getter;

@Getter
public enum Room {

    U126(38, "U126 Projektraum E"),
    U130(39, "U130 eLab"),
    U131(40, "Lab. auton. Systeme"),
    U204(41, "U204"),
    U205(42, "U205"),
    U206(43, "U206"),
    U207(44, "U207"),
    U210(45, "U210"),
    U212(46, "U212"),
    U214(47, "U214"),
    U215(48, "U215"),
    U216(297, "U216"),
    U225(49, "U225"),
    U226(50, "U226"),
    U227(51, "U227"),
    U304(52, "U304"),
    U305(53, "U305"),
    U306(4, "U306"),
    U307(55, "U307"),
    U310(56, "U310"),
    U311(57, "U311"),
    U312(58, "U312"),
    U313(59, "U313"),
    U314(60, "U314"),
    U315(61, "U315"),
    U325(82, "U325 DesignThinkingLab"),
    U326(63, "U326 nw-lab"),
    U327(64, "U327 db-lab"),
    U328(65, "U328 se-lab"),
    U329(66, "U329 cad-lab2"),
    U330(67, "U330"),
    U404(68, "U404"),
    U405(69, "U405"),
    U406(70, "U406"),
    U407(71, "U407"),
    U410(72, "U410"),
    U411(73, "U411"),
    U412(74, "U412"),
    U413(75, "U413"),
    U414(76, "U414"),
    U415(77, "U415"),
    U425(78, "U425 PC-Pool"),
    U426(79, "U426 PC-Pool"),
    U427(80, "U427 3D/CAD Lab/PC-Pool"),
    U428(81, "U428"),
    U429(62, "U429 PC-Pool"),
    U430(83, "U430 PC-Pool");

    private final int id;
    private final String name;

    Room(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static String getRoomIdStr(){
        StringBuilder sb = new StringBuilder();
        Room[] rooms = Room.values();
        for (int i = 0; i < rooms.length; i++) {
            sb.append(rooms[i].getId());
            if (i < rooms.length - 1) sb.append(",");
        }
        return sb.toString();
    }
}

