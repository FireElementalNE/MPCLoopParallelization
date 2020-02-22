import org.apache.commons.cli.*;
import org.apache.commons.lang3.SystemUtils;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.FileWriter;
import soot.Pack;
import soot.PackManager;
import soot.Transform;
import soot.options.SIOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class Main {

	private static String RT_PATH = Util.rt_path();
	private static String JCE_PATH = Util.jce_path();

	public void performAnalysis(String classpath, String klass, 
			String rtpath, String jcepath) {


		if(SystemUtils.IS_OS_WINDOWS) {
			Logger.info( "Running on Windows OS.");

		} else if(SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
			Logger.info( "Running on unix-like OS.");
		}
		classpath = classpath + Util.get_sep() + rtpath + Util.get_sep() + jcepath;

		Logger.debug(String.format("CLASSPATH: %s", classpath));
		Logger.debug(String.format("CLASS: %s", klass));

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
		args[i] = klass;

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

		// analysis.showResult();

		long endTime   = System.currentTimeMillis();
		String running_time_str = String.format("INFO: Total running time: %.2f sec", ((float)(endTime - startTime) / 1000));
		Logger.info(running_time_str);
	}

//	public void performAnalysis(String classpath, String klass) {
//		performAnalysis(classpath, klass, RT_PATH, JCE_PATH);
//	}

	public static void compile_program(String file) {
		boolean errors_found = false;
		try {
			Process p = Runtime.getRuntime().exec(String.format(Constants.COMPILE_CMD, file));
			BufferedReader stdError = new BufferedReader(new
					InputStreamReader(p.getErrorStream()));
			String s;
			while ((s = stdError.readLine()) != null) {
				Logger.error(s);
				errors_found = true;
			}
		} catch (IOException e) {
			Logger.error("Caught exception: " + e.getMessage());
			errors_found = true;
		}
		if(errors_found) {
			Logger.error(String.format("Found errors while compiling %s exiting.", file));
			System.exit(0);
		} else {
			Logger.info(String.format("Finished Compiling %s", file));
		}
	}

	public static void compile_programs() {
		File res = new File(Constants.RESOURCE_SRC);
		String[] files = res.list();
		compile_program(Constants.UTILS_JAVA_FILE);
		assert files != null;
		for (String file : files) {
			if(!Objects.equals(file, Constants.UTILS_JAVA_FILE)) {
				compile_program(file);
			}
		}
	}

	public static void main(String[] argv) {
		// TODO: fix this, the properties file does not work...
		Configurator.defaultConfig()
				.addWriter(new FileWriter("output.log"))
				.level(Level.DEBUG)
				.formatPattern("[{date}][{method}][{file}:{line}][{level}]: {message}")
				.activate();

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


		Option comp = Option.builder("compile")
				.hasArg(false)
				.longOpt("compile")
				.desc("compile test programs.")
				.required(false)
				.build();
		options.addOption(comp);


		CommandLineParser parser = new DefaultParser();
		HelpFormatter formatter = new HelpFormatter();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, argv);
		} catch (ParseException e) {
			Logger.error(e.getMessage());
			formatter.printHelp("utility-name", options);

			System.exit(1);
		}
		if(cmd.hasOption("compile")) {
			compile_programs();
		}
		String rtpath = cmd.getOptionValue("rtpath", RT_PATH);
		String jcepath = cmd.getOptionValue("jcepath", JCE_PATH);
		String classpath = cmd.getOptionValue("classpath");
		String klass = cmd.getOptionValue("class");
		Main m = new Main();



		m.performAnalysis(classpath, klass, rtpath, jcepath);
	}
}
