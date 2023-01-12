import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

/*
    COSC 3P71 - Assignment 3
    Shubham N. Amrelia
    sa19ss - 6846877
    December 4, 2022
 */

public class aco_tsp {

    String filename = "eil51.txt";
    int cityNo;
    double alpha = 0.2;
    double beta = 0.8;
    double evapRate = 0.95;
    int randomSeed = 0;
    int genSize = 80;
    int antSize = 50;
    ArrayList<Integer> xCord = new ArrayList<>();
    ArrayList<Integer> yCord = new ArrayList<>();
    double[][] phermone;
    int[][] distance;
    double[][] probMatrix;
    double[][] edgeMatrix;
    Random rand = new Random(randomSeed);
    ArrayList<ArrayList<Integer>> ants = new ArrayList<>();
    // keeping track of best of ant paths in each generation
    ArrayList<ArrayList<Integer>> bestInGens = new ArrayList<>();

    // 2 arraylists only for experimental purposes
    ArrayList<Integer> length_best = new ArrayList<>();
    ArrayList<Double> length_avg = new ArrayList<>();

    int currCity = -1;


    public aco_tsp() throws FileNotFoundException {

        File file = new File(filename);
        Scanner sc = new Scanner(file);

        for (int i = 1; i <= 6; i++) {
            sc.nextLine();
        }

        while (sc.hasNextLine()) {
            String[] arr = sc.nextLine().split(" ");
            if (Objects.equals(arr[0], "EOF")) break;
            xCord.add(Integer.parseInt(arr[1]));
            yCord.add(Integer.parseInt(arr[2]));
        }
        cityNo = xCord.size();
        edgeMatrix = new double[cityNo][cityNo];

        // initialize the matrices
        getPhermones();
        getDistance();
        setProbs();

        for (int i = 0; i < genSize; i++) {
            constructAntSolution();
            updatePheromones();
            setProbs();
            getBestPathAndWeight(ants, antSize);
            ants.clear();
        }

        System.out.println("\n\nBest in the run:\n");
        System.out.println("Best Path for the run: " + getBestPathAndWeight(bestInGens, genSize));
    }

    public ArrayList<Integer> getBestPathAndWeight(ArrayList<ArrayList<Integer>> allAnts, int antSize) {

        // arraylist for storing the amounts of pheromones of each path
        ArrayList<Double> pheromoneList = new ArrayList<>();

        for (int i = 0; i < antSize; i++) {
            double pher_sum = 0;
            for (int j = 0; j < (cityNo - 1); j++) {
                int x = allAnts.get(i).get(j);
                int y = allAnts.get(i).get(j + 1);

                // Takes sum of pheromones at j and j+1 indices for the current ant path
                pher_sum = pher_sum + phermone[x][y];
            }
            // adding the sum to the list at the end of each ant path iteration
            pheromoneList.add(pher_sum);
        }

        // index with the max amount of pheromones i.e., best path
        int maxPher = pheromoneList.indexOf(Collections.max(pheromoneList));

        // adding the best path for each generation
        if (allAnts.equals(ants)) bestInGens.add(allAnts.get(maxPher));

        ArrayList<Integer> weights = new ArrayList<>();
        // for loop to calculate the weight of the paths
        for (int i = 0; i < antSize; i++) {
            int w = 0;
            for (int j = 0; j < (cityNo - 1); j++) {
                int x = allAnts.get(i).get(j);
                int y = allAnts.get(i).get(j + 1);
                w = w + distance[x][y];
            }
            weights.add(w);
        }


        // taking average of the weights
        double avgWeight = (weights.stream()
                .mapToDouble(a -> a)
                .sum() / weights.size());
        if (allAnts.equals(ants)) System.out.println("\naverage tour length: " + avgWeight);
        System.out.println("best tour length: " + weights.get(maxPher));

        // recording the best and the avg tour lengths for experiment purposes
        length_best.add(weights.get(maxPher));
        length_avg.add(avgWeight);

        return allAnts.get(maxPher);
    }

    public void updatePheromones() {
        for (int i = 0; i < antSize; i++) {
            for (int j = 0; j < (cityNo - 1); j++) {
                int curr = ants.get(i).get(j);
                int next = ants.get(i).get(j + 1);
                edgeMatrix[curr][next] = edgeMatrix[curr][next] + 1;
                edgeMatrix[next][curr] = edgeMatrix[curr][next];
            }
        }

        for (int i = 0; i < cityNo; i++) {
            for (int j = 0; j < cityNo; j++) {

                phermone[i][j] = (1 - evapRate) * phermone[i][j];
                edgeMatrix[i][j] = edgeMatrix[i][j] / cityNo;
                edgeMatrix[i][j] = phermone[i][j] + edgeMatrix[i][j];
            }
        }
    }

    public void getDistance() {
        // Euclidean distances
        distance = new int[xCord.size()][xCord.size()];
        for (int i = 0; i < xCord.size(); i++) {
            for (int j = 0; j < xCord.size(); j++) {
                double dist = Math.sqrt(Math.pow((yCord.get(j) - yCord.get(i)), 2) + Math.pow((xCord.get(j) - xCord.get(i)), 2));
                distance[i][j] = (int) Math.round(dist);
                // if it is the same city, set the distances to a huge number "like" infinity
                if (i == j) distance[i][j] = 99999999;

            }
        }
    }

    public void setProbs() {
        // PROBABILITY stuff here
        probMatrix = new double[xCord.size()][xCord.size()];
        for (int i = 0; i < xCord.size(); i++) {
            for (int j = i; j < yCord.size(); j++) {
                probMatrix[i][j] = (Math.pow(phermone[i][j], alpha) * Math.pow(1.0 / distance[i][j], beta));
                probMatrix[j][i] = probMatrix[i][j];
            }
        }
    }

    public void getPhermones() {
        phermone = new double[xCord.size()][xCord.size()];
        for (int i = 0; i < xCord.size(); i++) {
            for (int j = i; j < yCord.size(); j++) {
                if (i != j) {
                    phermone[i][j] = rand.nextDouble(0, 1);
                    phermone[j][i] = phermone[i][j];
                }
            }
        }
    }

    public void constructAntSolution() {
// ants: an arraylist of an arraylist of all the "paths"
        // temp is the path for the current ant, and it resets on every iteration
        for (int gen = 0; gen < antSize; gen++) {
            int cntr = 0;
            ArrayList<Integer> temp = new ArrayList<>();
            // for loop for making one path at a time
            while (temp.size() != cityNo) {

                if (cntr == 0) {
                    // NOTE: bound is 51 as I am directly getting the index of the city which would be "random number -1"
                    currCity = rand.nextInt(0, cityNo);
                    temp.add(currCity);
                }

                if (cntr != 0) currCity = temp.get(temp.size() - 1);

                // to print the iteration's status
//                System.out.println(cntr + ", Current city: " + currCity + ", temp's latest index: " + temp.get(temp.size() - 1));

                // making a copy of the probability matrix of the current city index
                ArrayList<Double> copyProb = new ArrayList<>();
                for (int i = 0; i < cityNo; i++) {
                    copyProb.add(probMatrix[currCity][i]);
                }
//            System.out.println("copyProb's size: "+copyProb.size());

                // sum of probs
                double allProb = copyProb.stream()
                        .mapToDouble(a -> a)
                        .sum();

                // now we set the new probabilities
                copyProb.replaceAll(aDouble -> aDouble / (allProb));

                // now we loop through the new set probs and "roll a dice"
                Random ranum = new Random();
                // make cumu_sum equal to the first prob
                double cumu_sum = -1.0;
                if (currCity == 0) cumu_sum = copyProb.get(1);
                if (currCity != 0) cumu_sum = copyProb.get(0);
                double dice = ranum.nextDouble(0, 1);

                for (int i = 0; i < copyProb.size(); i++) {
                    // make sure that the index is legal i.e. P11 and P1,previous node will not be considered
                    if (i != currCity && !temp.contains(i)) {

                        //check if cumu_sum is less than dice and add the current probability if YES

                        if (cumu_sum < dice && i != 0) cumu_sum = cumu_sum + copyProb.get(i);
                            // else you just add the index to the ant's path and break out
                        else {
                            temp.add(i);
                            break;
                        }
                    }
                }// inner for loop that checks probability ends
                cntr++;
            }// one path finishes

            // adding the current path arraylist to the final arraylist
            ants.add(temp);
        }
    }

    // call this method at the end of constructor ONLY iff getting values for experiments
    public void expValues(){
        System.out.println("Best Weights: \n");
        for (int i = 0; i < length_best.size(); i++) {
            System.out.println(length_best.get(i));
        }

        System.out.println("Avg Weights: \n");
        for (int i = 0; i < length_avg.size(); i++) {
            System.out.println(length_avg.get(i));
        }
    }

    public static void main(String[] args) throws FileNotFoundException {
        new aco_tsp();

    }
}
