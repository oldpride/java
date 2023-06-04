// https://web.mit.edu/java_v1.0.2/www/tutorial/java/cmdLineArgs/parsing.html

public class CmdArgs {
    public static void usage(String msg) {
        System.err.println(msg);
        System.err.println("Usage: java CmdArgs [-verbose] [-xn] [-count int] [-output afile] filename");
        System.err.println("example: java CmdArgs -verbose -xn -count 123 -output out_file pos_file");
        System.exit(1);
    }

    public static void main(String[] args) {
        int i = 0, j;
        String arg;
        char flag;
        boolean vflag = false;
        String outputfile = "";

        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];

            // use this type of check for "wordy" arguments
            if (arg.equals("-verbose")) {
                System.out.println("verbose mode on");
                vflag = true;
            }

            // use this type of check for arguments that require arguments
            else if (arg.equals("-output")) {
                if (i < args.length)
                    outputfile = args[i++];
                else
                    usage("-output requires a filename");
                if (vflag)
                    System.out.println("output file = " + outputfile);
            }

            else if (arg.equals("-count")) {
                if (i < args.length) {
                    String number_string = args[i++];
                    int count = 0;
                    try {
                        count = Integer.parseInt(number_string);

                    } catch (NumberFormatException e) {
                        usage("Argument -count must be an integer. but got " + number_string);
                    }

                    if (vflag)
                        System.out.println("count = " + count);
                } else
                    usage("-output requires a filename");

            }

            // use this type of check for a series of flag arguments
            else {
                for (j = 1; j < arg.length(); j++) {
                    flag = arg.charAt(j);
                    switch (flag) {
                        case 'x':
                            if (vflag)
                                System.out.println("Option x");
                            break;
                        case 'n':
                            if (vflag)
                                System.out.println("Option n");
                            break;
                        default:
                            usage("ParseCmdLine: illegal option " + flag);
                            break;
                    }
                }
            }
        }
        if (i == args.length)
            // missing positional argument
            usage("missing positional argument");
        else
            System.out.println("Success!");

    }
}
