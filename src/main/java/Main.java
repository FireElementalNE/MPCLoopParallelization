import org.apache.commons.cli.*;
import org.apache.commons.lang3.SystemUtils;
import org.tinylog.Logger;
import soot.Pack;
import soot.PackManager;
import soot.Transform;
import soot.options.Options;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;

public class Main {

	private static final String RT_PATH = Utils.rt_path();
	private static final String JCE_PATH = Utils.jce_path();

	/**
	 * compile a java program
	 * @param classname the program name
	 */
	private static void compile_program(String classname) {
		String cmd = String.format(Constants.DEFAULT_COMPILE_CMD, classname);
		Utils.execute_cmd(cmd);
	}

	/**
	 * main method
	 * @param argv arguments
	 */
	public static void main(String[] argv) {
		// needed fix...

		System.setProperty("tinylog.configuration", "tinylog.properties");
		Utils.refresh_dir(Constants.GRAPH_DIR);
		Utils.refresh_dir(Constants.Z3_DIR);

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
				.required(false)
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
			Logger.error(e.getMessage());
			formatter.printHelp("utility-name", options);
			System.exit(1);
		}
		String classpath = cmd.getOptionValue("classpath", Constants.DEFAULT_CP);
		String rtpath = cmd.getOptionValue("rtpath", Constants.DEFAULT_RT_PATH);
		String jcepath = cmd.getOptionValue("jcepath", Constants.DEFAULT_JCE_PATH);

		String klass = cmd.getOptionValue("class");

		if(SystemUtils.IS_OS_WINDOWS) {
			Logger.info( "Running on Windows OS.");

		} else if(SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_LINUX) {
			Logger.info( "Running on unix-like OS.");
		}
		classpath = classpath + File.pathSeparator + rtpath + File.pathSeparator + jcepath;
		compile_program(klass);
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
		Options.v().set_keep_line_number(true);
		// Code hooks the Analysis then launches Soot, which traverses
		PackManager pm = PackManager.v();
		Pack pack = pm.getPack("stp");

		Analysis analysis = new Analysis(klass);
		Transform t = new Transform("stp.arrayssa", analysis);
		//p.insertAfter(t, phaseName);
		//p.insertAfter(t, "sop.cpf");
		pack.add(t);

		soot.Main.main(args.toArray(new String[0]));

		//        String outputDir = SourceLocator.v().getOutputDir();

		// analysis.showResult();

		long endTime   = System.currentTimeMillis();
		Logger.info(String.format("Total running time: %.2f sec", ((float)(endTime - startTime) / 1000)));
		try {
			List<MemoryPoolMXBean> pools = ManagementFactory.getMemoryPoolMXBeans();
			double heap_mem_used = 0.0d;
			double heap_mem_com = 0.0d;
			double non_heap_mem_used = 0.0d;
			double non_heap_mem_com = 0.0d;
			for (MemoryPoolMXBean pool : pools) {
				MemoryUsage peak = pool.getPeakUsage();
				if(pool.getType() == MemoryType.NON_HEAP) {
					non_heap_mem_used += peak.getUsed();
					non_heap_mem_com += peak.getCommitted();

				} else {
					heap_mem_used += peak.getUsed();
					heap_mem_com += peak.getCommitted();
				}
			}
			Logger.info(String.format("Peak HEAP used: %.2f MB", heap_mem_used * 1e-6));
			Logger.info(String.format("Peak HEAP memory reserved: %.2f MB", heap_mem_com * 1e-6));
			Logger.info(String.format("Peak NON-HEAP used: %.2f MB", non_heap_mem_used * 1e-6));
			Logger.info(String.format("Peak NON-HEAP memory reserved: %.2f MB", non_heap_mem_com * 1e-6));

		} catch (Throwable e) {
			Logger.error("Caught " + e.getClass().getSimpleName() + ": " + e.getMessage());
			if(Constants.PRINT_ST) {
				e.printStackTrace();
			}
		}
	}
}
