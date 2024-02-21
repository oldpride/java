   from vscode's left-bottom brick: Java Projects
   click "+" to add a new project
   choose "no build tools"
   choose location to github/java/examples/00_no_build_tools
   $ find .
   .
   ./bin
   ./README.md
   ./lib
   ./.vscode
   ./.vscode/settings.json
   ./src
   ./src/App.java
   $ javac src/App.java -d bin
   this created ./bin/App.Class

   $ java -cp bin App
   Hello, World!