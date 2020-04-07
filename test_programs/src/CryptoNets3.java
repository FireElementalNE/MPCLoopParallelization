class CryptoNets3 {
    public static void main(String[] args) {
        int[] res_layer = new int[100];
        int[] OUTPUT_res = new int[100];
        int output_size = Integer.parseInt(args[1]);
        int o = 0;
        for(int i = 0; i < output_size; i++) {
            OUTPUT_res[o*output_size+i] = res_layer[i];
        }

    }
}

