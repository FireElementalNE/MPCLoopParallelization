class Test17 {
    public static void main(String[] args) {
        int[] A = new int[10];
        int z = -1;
        for(int i = 0; i < 10; i++) {
            int p = i * z;
            A[i - 2 + z] = 10;
            z = z - 1;
        }
    }
}