package com.tpsup.tpdist;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Cmdline {
	public static String header = "\n" + "Usage:\n" + "\n" + "   tpdist in powershell\n" + "\n"
			+ "   normal mode: server waits to be pulled; client pulls.\n" + "     tpdist server local_port\n"
			+ "     tpdist client remote_host remote_port remote_path1 remote_path2 ... local_dir\n" + "\n"
			+ "   reversed mode: server waits to take in data; client pushes.\n"
			+ "     tpdist server local_port  -reverse remote_port remote_path1 remote_path2 ... local_dir\n"
			+ "     tpdist client remote_host remoe_port -reverse\n" + "\n"
			+ "   If remote path is a relative path, it will be relative to remote user's home dir.\n";

	public static String footer = "  as a server\n" + "     java -jar tpdist.jar server 5555\n" + "\n"
			+ "  as a client\n"
			+ "     java -jar tpdist.jar client localhost 5555 /cygdrive/c/Users/william/github/tpsup/ps1 tmp \n" + "\n"
			+ "     java -jar tpdist.jar client localhost 5555 'C:/users/william/github/tpsup/ps1' 'C:/users/william/github/tpsup/kdb' /tmp"
			+ "\n";

	public static void usage(String message, Options options) {
		if (message != null) {
			MyLog.append(MyLog.ERROR, message);
		}

		// printHelp() only prints to console. we need to collect string so that we can
		// redirect it.
		// https://stackoverflow.com/questions/44426626/how-do-i-get-help-string-from-commons-cli-instead-of-print
		HelpFormatter formatter = new HelpFormatter();
		StringWriter stringwriter = new StringWriter();
		PrintWriter printwriter = new PrintWriter(stringwriter);

		formatter.printHelp(printwriter, 80, "tpdist", header, options, formatter.getLeftPadding(),
				formatter.getDescPadding(), footer, true);
		printwriter.flush();

		MyLog.append(stringwriter.toString());
		System.exit(1);
	}

	public static Options setOptions() {
		// https://commons.apache.org/proper/commons-cli/usage.html
		Options options = new Options();

		// switches, boolean is false
		options.addOption("h", "help", false, "print help message");
		options.addOption("v", "verbose", false, "verbose mode");
		options.addOption("r", "reverse", false, "reverse mode, server to pull. default is client to pull");
		options.addOption("n", "dryrun", false, "dryrun mode");
		options.addOption("d", "diff", false, "diff mode = dryrun mode + diff");
		options.addOption("deep", false, "deep-check mode, always use cksum, very slow");
		options.addOption("KeepTmpFile", false, "Keep the tmp file, for troubleshooting purpose");

		// args, boolean is true
		options.addOption("m", "matches", true, "regex file match pattern. multiple patterns separated by comma ','");
		options.addOption("x", "excludes", true,
				"regex file exclude pattern. multiple patterns separated by comma ','");
		options.addOption("timeout", true, "INT. expect will time out if pattern not matched within this much time");
		options.addOption("idle", true, "INT. listener will quit after this much time of idle");
		options.addOption("allowhost", true,
				"named file contains host/ip allowed to connect to us (server mode), one line per host/ip");
		options.addOption("denyhost", true,
				"named file contains host/ip not allowed to connect to us (server mode), one line per host/ip");
		options.addOption("allowfile", true,
				"named file contains file pattern (regex) allowed to be pulled, one pattern per line");
		options.addOption("denyfile", true,
				"named file contains file pattern (regex) not allowed to be pulled, one pattern per line");
		options.addOption("enc", "encode", true, "encode key string (seed) ");
		options.addOption("maxsize", true, "INT. maximium size to be transferred. default -1, ie, unlimited");
		options.addOption("maxtry", true, "INT. maximium tries of client when connect to server, default to 5");
		options.addOption("tmpdir", true, "tmpdir. default to " + Env.tmpBase);


		return options;
	}

	public static void main(String[] args) {
		// MyLog.verbose = true;
		//MyLog.append("args = " + MyGson.toJson(args));
		Options options = setOptions();
		ArrayList<String> optionList = new ArrayList<String>();

		MyLog.append(MyLog.VERBOSE, "configured options: ");

		for (Option o : options.getOptions()) {
			String oName = o.getLongOpt();
			if (oName == null) {
				oName = o.getOpt();
			}
			optionList.add(oName);
			// MyLog.append(MyGson.toJson(o));
			MyLog.append(MyLog.VERBOSE,
					"   name = " + o.getOpt() + " " + o.getLongOpt() + " " + o.getType().toString() + " " + o.hasArg());
		}

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;

		try {
			cmd = parser.parse(options, args);
		} catch (ParseException e) {
			usage("parse error: " + e.getMessage(), options);
		}

		HashMap<String, Object> opt = new HashMap<String, Object>();

		// for (String o : optionList) {
		for (Option option : cmd.getOptions()) {
			// Option option = options.getOption(o);
			String name = option.getLongOpt();
			if (name == null) {
				name = option.getOpt();
			}
			if (option.hasArg()) {
				if (option.getDescription().startsWith("INT")) {
					opt.put(name, new Integer(cmd.getOptionValue(name)));
				} else {
					opt.put(name, cmd.getOptionValue(name));
				}
			} else {
				opt.put(name, cmd.hasOption(name));
			}
		}

		Env.verbose = (Boolean) opt.getOrDefault("verbose", false);

		if (opt.containsKey("help")) {
			usage(null, options);
		}

		ArrayList<String> argv = new ArrayList<String>(cmd.getArgList());

		MyLog.append(MyLog.VERBOSE, "opt = " + MyGson.toJson(opt));
		MyLog.append(MyLog.VERBOSE, "positional args " + MyGson.toJson(cmd.getArgs()));

		if (argv.size() == 0) {
			usage("wrong number of args", options);
		}

		String role = argv.remove(0);

		if (role.equals("server")) {

		} else if (role.equals("client")) {
			if (argv.size() < 2) {
				usage("wrong number of args", options);
			}
			String host = argv.remove(0);
			int port = new Integer(argv.remove(0));

			if (opt.containsKey("reverse")) {
				if (argv.size() != 0) {
					usage("wrong number of args", options);
				}
				Client client = new Client(host, port, opt);
				if (client.myconn == null) {
					return;
				}

			} else {
				if (argv.size() < 2) {
					usage("wrong number of args", options);
				}
				Client client = new Client(host, port, opt);
				if (client.myconn == null) {
					return;
				}
				String local_dir = argv.remove(argv.size() - 1);
				ArrayList<String> remote_paths = argv;
				ToPull.pull(client.myconn, remote_paths, local_dir, opt);
			}
		} else {
			usage("unknown role=" + role, options);
		}
	}

}
