class MNIST2 {
    public static void main(String[] args) {
        int[] kernel = new int[100];
        int[] image = new int[100];
        int window_size = Integer.parseInt(args[1]);
        int wx = Integer.parseInt(args[2]);
        int wy = Integer.parseInt(args[3]);
        int y = Integer.parseInt(args[4]);
        int x = Integer.parseInt(args[5]);
        int stride = Integer.parseInt(args[6]);
        int image_width = Integer.parseInt(args[7]);
        int tmp = 0;
        for(wx = 0; wx < window_size; wx++) {
            int convPos = wx+wy*window_size;
            tmp += kernel[convPos] * image[(y*stride + wy) * image_width + (x*stride + wx)];
        }
    }
}