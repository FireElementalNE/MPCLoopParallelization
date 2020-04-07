class CryptoNets2 {
    public static void main(String[] args) {
        int[] input_layer = new int[100];
        int[] vals = new int[100];
        int size = Integer.parseInt(args[1]);
        int o = 0;
        for(int i = 0; i < size; i++) {
            input_layer[i] = vals[o*size+i];
        }
    }
}

