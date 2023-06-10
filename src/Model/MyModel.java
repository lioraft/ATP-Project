package Model;
import Client.IClientStrategy;
import IO.MyDecompressorInputStream;
import Server.*;
import algorithms.mazeGenerators.Maze;
import Client.*;
import algorithms.search.Solution;

import java.util.concurrent.atomic.AtomicReference;
import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class MyModel implements IModel{
    private Server mazeGeneratingServer; // maze generating server
    private Server solveSearchProblemServer; // solve search problem server
    public MyModel() {
        // generate maze generating server
        mazeGeneratingServer = new Server(5400, 1000, new ServerStrategyGenerateMaze());
        // generate solve search problem server
        solveSearchProblemServer = new Server(5401, 1000, new ServerStrategySolveSearchProblem());
    }

    // function to generate maze based on dimensions and return it
    public Maze generateMaze(int width, int height) {
        // start server
        mazeGeneratingServer.start();
        AtomicReference<Maze> mazeRef = new AtomicReference<>(); // Use AtomicReference to hold the maze object
        try {
            Client client = new Client(InetAddress.getLocalHost(), 5400, new IClientStrategy() {
                @Override
                public void clientStrategy(InputStream inFromServer, OutputStream outToServer) {
                    try {
                        ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                        ObjectInputStream fromServer = new ObjectInputStream(inFromServer);
                        toServer.flush();
                        int[] mazeDimensions = new int[]{width, height};
                        toServer.writeObject(mazeDimensions);
                        // send maze dimensions to server
                        toServer.flush();
                        byte[] compressedMaze = (byte[]) fromServer.readObject();
                        // read generated maze (compressed with MyCompressor) from server
                        InputStream is = new MyDecompressorInputStream(new ByteArrayInputStream(compressedMaze));
                        byte[] decompressedMaze = new byte[3000 /*giving more space*/];
                        // allocating byte[] for the decompressed maze
                        is.read(decompressedMaze); // fill decompressedMaze with bytes
                        Maze maze = new Maze(decompressedMaze);
                        mazeRef.set(maze); // set the maze object in the AtomicReference
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            client.communicateWithServer();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return mazeRef.get(); // return the maze object from the AtomicReference
    }

    // function to solve maze and return solution
    public Solution solveMaze(Maze maze) {
        // start server
        solveSearchProblemServer.start();
        AtomicReference<Solution> mazeSolution = new AtomicReference<>();
        try {
            Client client = new Client(InetAddress.getLocalHost(), 5401, new IClientStrategy() {
                @Override
                public void clientStrategy(InputStream inFromServer, OutputStream outToServer) {
                    try {
                        ObjectOutputStream toServer = new ObjectOutputStream(outToServer);
                        ObjectInputStream fromServer = new ObjectInputStream(inFromServer);
                        toServer.flush();
                        toServer.writeObject(maze); // send maze to server
                        toServer.flush();
                        mazeSolution.set((Solution) fromServer.readObject()); // read generated maze (compressed with MyCompressor) from server
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
            client.communicateWithServer();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return mazeSolution.get();
    }


}
