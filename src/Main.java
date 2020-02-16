import org.apache.commons.cli.*;

import org.apache.commons.lang3.SystemUtils;
import soot.Pack;
import soot.PackManager;
import soot.Transform;

public class Main {

	private static LogWriter logWriter = new LogWriter(Main.class.getName(), Constants.DEBUG);

	private static String RT_PATH = Util.rt_path();
	private static String JCE_PATH = Util.jce_path();

	public void performAnalysis(String classpath, String klass, 
			String rtpath, String jcepath) {


		if(SystemUtils.IS_OS_WINDOWS) {
			logWriter.write_out("Running on Windows OS.");

		} else if(SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
			logWriter.write_out("Running on unix-like OS.");
		}
		classpath = classpath + Util.get_sep() + rtpath + Util.get_sep() + jcepath;

		logWriter.write_out(String.format("CLASSPATH: %s", classpath));
		logWriter.write_out(String.format("CLASS: %s", klass));

		String[] args = new String[18];
		int largeValue = 1000000;
		int i = 0;

		args[i++] = "-w";
		args[i++] = "-p";
		args[i++] = "wjop";
		args[i++] = "on";
		args[i++] = "-p";
		args[i++] = "wjop.si";
		args[i++] = "expansion-factor:" + largeValue;
		args[i++] = "-p";
		args[i++] = "wjop.si";
		args[i++] = "max-container-size:" + largeValue;
		args[i++] = "-p";
		args[i++] = "wjop.si";
		args[i++] = "max-inlinee-size:" + largeValue;
		// -f J causes Soot to write out .jimple files. The default output directory is sootOutput
		// which is usually located in the same directory as src.
		args[i++] = "-f";
		args[i++] = "shimple"; //args[i++] = "J";

		// -cp specifies the class path. Must include a path to the application classes, and the rt.jar
		args[i++] = "-cp";
		args[i++] = classpath;
		// specifies the class that contains the "main" method
		args[i++] = klass;

		long startTime = System.currentTimeMillis();

		// Code hooks the Analysis then launches Soot, which traverses 
		PackManager pm = PackManager.v();
		Pack p = pm.getPack("stp");

		//        Iterator<Transform> iterator = stp.iterator();
		//        Transform last = null;
		//        while(iterator.hasNext()) {
		//          last = iterator.next();
		//        }

		//        String phaseName = last.getPhaseName();
		Analysis analysis = new Analysis(); 
		Transform t = new Transform("stp.mixedprotocols", analysis);

		//p.insertAfter(t, phaseName);
		//p.insertAfter(t, "sop.cpf");
		p.add(t);

		soot.Main.main(args);

		//        String outputDir = SourceLocator.v().getOutputDir();

		analysis.showResult();

		long endTime   = System.currentTimeMillis();
		String running_time_str = String.format("INFO: Total running time: %.2f sec", ((float)(endTime - startTime) / 1000));
		logWriter.write_out(running_time_str);
	}

	public void performAnalysis(String classpath, String klass) {
		performAnalysis(classpath, klass, RT_PATH, JCE_PATH);
	}

	public static void main(String[] argv) {
		Options options = new Options();

		Option r = Option.builder("r")
				.hasArg()
				.longOpt("rtpath")
				.desc("complete path to rt.jar, default: " + RT_PATH)
				.required(false)
				.build();
		options.addOption(r);

		Option j = Option.builder("j")
				.hasArg()
				.longOpt("jcepath")
				.desc("complete path to jce.jar, default: " + JCE_PATH)
				.required(false)
				.build();
		options.addOption(j);

		Option cp = Option.builder("cp")
				.hasArg()
				.longOpt("classpath")
				.desc("path to the class to analyze")
				.required(true)
				.build();
		options.addOption(cp);


		Option c = Option.builder("c")
				.hasArg()
				.longOpt("class")
				.desc("name of the class to analyze")
				.required(true)
				.build();
		options.addOption(c);

		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, argv);
		} catch (ParseException e) {
			logWriter.write_err(e.getMessage());
			formatter.printHelp("utility-name", options);

			System.exit(1);
		}

		String rtpath = cmd.getOptionValue("rtpath", RT_PATH);
		String jcepath = cmd.getOptionValue("jcepath", JCE_PATH);
		String classpath = cmd.getOptionValue("classpath");
		String klass = cmd.getOptionValue("class");
		Main m = new Main();
		m.performAnalysis(classpath, klass, rtpath, jcepath);
	}
}
