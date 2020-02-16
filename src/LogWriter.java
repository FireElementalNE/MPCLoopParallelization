import java.text.SimpleDateFormat;
import java.util.Date;

class LogWriter {
    private String class_name;
    private boolean debug;

    /**
     * constructor
     * @param class_name the name of the class that is using the logger
     */
    LogWriter(String class_name, boolean debug) {
        this.class_name = class_name;
        this.debug = debug;
    }

    /**
     * create a data stamped string for the message
     * @param msg the message
     * @return a date stamped string of the message (which
     *  also include the method name and the class name
     */
    private  String make_date_msg(String msg, int stack_trace_num) {
        Thread t = Thread.currentThread();
        String method_name = new Throwable().getStackTrace()[stack_trace_num].getMethodName();
        SimpleDateFormat formatter = new SimpleDateFormat("d/M/y HH:mm:ss:SSS");
        Date date = new Date();
        return String.format("[%d][%s][%s][%s]: %s%n", t.getId(), formatter.format(date), class_name, method_name, msg);
    }

    private void print_out(String msg, int stack_trace_num) {
        String date_msg = make_date_msg(msg, stack_trace_num);
        if(debug) {
            System.out.print(date_msg);
        }
    }

    private void print_err(String msg) {
        String date_msg = make_date_msg(msg, 3);
        System.err.print(date_msg);
    }

    void force_write_out(String msg) {
        String date_msg = make_date_msg(msg, 3);
        System.out.print(date_msg);
    }



    /**
     * write a msg to stdout
     * @param msg the message
     */
    void write_out(String msg) {
        print_out(msg, 3);
    }


    /**
     * write a msg to stderr
     * @param msg the message
     */
    void write_err(String msg) {
        print_err(msg);
    }

    /**
     * write error msg and exit
     * @param msg the msg
     */
    void exit_with_error(String msg) {
        print_err(msg);
        System.exit(0);
    }
}