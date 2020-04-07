class CryptoNets5 {
    public static void main(String[] args) {
        int IMAGE_WIDTH = Integer.parseInt(args[1]);
        int padded_width = IMAGE_WIDTH+2;
        int[] convolution_input = new int[padded_width*padded_width];
        for(int i = 0; i < padded_width; i++) {
            convolution_input[i] = 0;
            convolution_input[i+padded_width] = 0;
            convolution_input[padded_width*i] = 0;
            convolution_input[padded_width*i+1] = 0;
        }
    }
}

