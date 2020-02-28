import org.apache.commons.cli.*;
import org.apache.commons.lang3.SystemUtils;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.FileWriter;
import soot.Pack;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Main {

	private static String RT_PATH = Utils.rt_path();
	private static String JCE_PATH = Utils.jce_path();

	private static void compile_program(String file) {
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

	private static void compile_programs() {
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

		org.apache.commons.cli.Options options = new org.apache.commons.cli.Options();

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

		if(SystemUtils.IS_OS_WINDOWS) {
			Logger.info( "Running on Windows OS.");

		} else if(SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
			Logger.info( "Running on unix-like OS.");
		}
		classpath = classpath + Utils.get_sep() + rtpath + Utils.get_sep() + jcepath;

		Logger.debug(String.format("CLASSPATH: %s", classpath));
		Logger.debug(String.format("CLASS: %s", klass));

		List <String> args = new ArrayList<>();
		args.add("-w");
		args.add("-p");
		args.add("wstp");
		args.add("on");
		// -f J causes Soot to write out .jimple files. The default output directory is sootOutput
		// which is usually located in the same directory as src.
		args.add("-f");
		args.add("shimple"); //args[i++] = "J";

		// -cp specifies the class path. Must include a path to the application classes, and the rt.jar
		args.add("-cp");
		args.add(classpath);
		// specifies the class that contains the "main" method
		args.add(klass);

		long startTime = System.currentTimeMillis();
		Options.v().set_whole_shimple(true);

		// Code hooks the Analysis then launches Soot, which traverses
		PackManager pm = PackManager.v();
		Pack p = pm.getPack("stp");

		Analysis analysis = new Analysis();
		Transform t = new Transform("stp.arrayssa", analysis);
		//p.insertAfter(t, phaseName);
		//p.insertAfter(t, "sop.cpf");
		p.add(t);

		soot.Main.main(args.toArray(new String[0]));

		//        String outputDir = SourceLocator.v().getOutputDir();

		// analysis.showResult();

		long endTime   = System.currentTimeMillis();
		String running_time_str = String.format("INFO: Total running time: %.2f sec", ((float)(endTime - startTime) / 1000));
		Logger.info(running_time_str);
	}
}
