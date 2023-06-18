package service.model.enums;

import static util.IOUtil.readInput;

public enum Operation {
    UPDATE(1, "Atualizar"),
    SEARCH(2, "Procurar"),
    DOWNLOAD(3, "Baixar"),
    EXIT(0, "Sair");

    private final Integer code;
    private final String formattedName;

    Operation(int code, String formattedName) {
        this.code = code;
        this.formattedName = formattedName;
    }

    public static Operation valueOf(Integer code) throws IllegalArgumentException {
        switch (code) {
            case 1: return UPDATE;
            case 2: return SEARCH;
            case 3: return DOWNLOAD;
            case 0: return EXIT;
            default: throw new IllegalArgumentException("Opção inválida selecionada!");
        }
    }

    public static Operation read() throws IllegalArgumentException {
        print();

        return valueOf(Integer.valueOf(readInput("Selecione uma opção: ")));
    }

    public static void print() {
        System.out.print("\r");

        for (Operation operation : Operation.values()) {
            System.out.printf("%d - %s\n", operation.getCode(), operation.getFormattedName());
        }

        System.out.print("\n");
    }

    public static void reprint() {
        System.out.println();

        print();

        System.out.println("Selecione uma opção: ");
    }

    public int getCode() {
        return code;
    }

    public String getFormattedName() {
        return formattedName;
    }
}
