class CryptoNets1 {
    public static void main(String[] args) {
        int cols_res = 20;
        int i = 0;
        int[] vals = new int[100];
        int[] OUTPUT_res = new int[100];
        int cols = 10;
//        for(int k = 0; k < 100; k++) {
//            vals[k] = 1;
//            OUTPUT_res[k] = 1;
//        }
        for(int j = 0; j < cols_res; j++) {
            int x = j * 2;
            int y = i * 2;
            int max = vals[y* cols + x];
            if(vals[y*cols + x + 1] > max) {
                max = vals[y*cols + x + 1];
            }
            if(vals[(y + 1) *cols + x] > max) {
                max = vals[(y + 1) * cols + x];
            }
            if(vals[(y + 1) *cols + x + 1] > max) {
                max = vals[(y + 1) * cols + x + 1];
            }
            OUTPUT_res[i * cols_res + j] = max;
        }
    }
}

// c1 = CMP(vals[y*cols + x + 1] > max)
// max = MUX(c1, max, vals[y*cols + x + 1])
// c2 = CMP(vals[(y + 1) *cols + x] > max)
// max = MUX(c1, max, vals[(y + 1) * cols + x])
// c3 = CMP(vals[(y + 1) *cols + x + 1] > max)
// max = vals[(y + 1) * cols + x + 1]