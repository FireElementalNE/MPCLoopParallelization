class Test21 {
    public static void main(String[] args) {
        int a = 1;
        for(int i = 0; i < 20; i++) {
            if(a % 3 == 1) {
                a = a + 10;
            } else {
                a = a - 10;
            }
        }
    }
}