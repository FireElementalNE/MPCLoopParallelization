class MNIST1 {
    public static void main(String[] args) {
        int[] vals = new int[100];
        int[] OUTPUT_res = new int[100];
        int cols = 100;
        int i = 0;
        int cols_res = 0;
        for(int j = 0; j < cols_res; j++) {
            int x = j * 2;
            int y = i * 2;
            int max = vals[y*cols + x];
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

