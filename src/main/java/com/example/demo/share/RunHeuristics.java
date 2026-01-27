package com.example.demo.share;

import com.example.demo.utils.InputData;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class RunHeuristics {

    public static void main(String[] args) throws Exception {

        // TU002C5C5.txt
        String instances = "JP1G";  //待求解的样例列表

        // 当前样例路径:
        File dirSingle = new File("D:/RESEARCH/DJD/NestingData/");

        // 所有样例的路径:
        File dirAllInstance = new File("D:/RESEARCH/DJD/NestingData/");

        // 所有结果存储:
        File dirSolution = new File("D:/RESEARCH/DJD/results1/" + instances + "/");

        // 当前结果存储:
//        File filePath = new File("C:/Users/Fortune/Desktop/results/");
        File filePath = new File("D:/develop/BPP-server/demo/src/main/resources/results/");

        // 参数
        int numHeuristics = 1;            // 使用的启发式算法个数，如MALBR, MALR, MABR, ...
        // 原DJD中只用了BL
        // 改进版本用的是MALBR

        boolean repetition = true;        // 覆盖之前的结果
        boolean graphVisual = true;        // 可视化图结果

        // 结果存储路径
        if (!filePath.exists())
            filePath.mkdir();            //如果文件夹不存在，新建一个

        // 如果结果路径不存在，新建文件夹
        if (!dirSolution.exists())
            dirSolution.mkdir();

        File archieveProblems = new File(dirSingle, instances + ".txt");

        System.out.println("Solving instances: " + instances);
        RunHeuristics.run(dirAllInstance, dirSolution, filePath, archieveProblems,
                instances, numHeuristics, repetition, graphVisual);
        System.out.println("Finish");
        System.out.println();
    }

    public RunHeuristics() {
    }

    public static List<Sheet> run_input_data(int numHeuristicas, InputData inputData) throws Exception {
        List<Sheet> sheets = new ArrayList<Sheet>();
        RWfiles rw = new RWfiles();
        List<String> problems = new ArrayList<String>();


        problems.add("problem");
        int numproblems = problems.size();
        double[][] aptitudes = new double[numproblems][numHeuristicas];  //每个样例的F值
        int[][] numObjects = new int[numproblems][numHeuristicas];       //每个样例所用的底板数量
        int[][] executionTime = new int[numproblems][numHeuristicas];   //每个样例的执行时间
        double[][] utilizations = new double[numproblems][numHeuristicas]; // 每个样例的利用率
        double[][] Kvalues = new double[numproblems][numHeuristicas];  // 每个样例的K值

        int indice = 0;
        Iterator<String> iter = problems.iterator(); // problems的数量就是listOfInstances中问题的数量，人为设定
        while (iter.hasNext()) {
            Instance p = new Instance(numproblems * numHeuristicas);    //为每个样例及对应的方法生成一个instance求解
            String problem = iter.next();
            // 应用各种启发式求解
            for (int i = 0; i < numHeuristicas; i++) {
                System.out.println("Solving " + problem + " with heuristic " + i);

                // 读取文件，分为txt和xlxs
//                        if (problem.endsWith("txt")) {
//                            p.obtainProblem(new File(dirAllInstance, problem));
//                        } else { //excel文件
//                            p.obtainProblemExcel(new File(dirAllInstance, problem));
//                        }
                p.obtainProblemInfo(inputData);


                // 开始计算，起始时间
                long start = System.currentTimeMillis();
                // TODO:这里控制采用哪种方法
//                        aptitudes[indice][i] = p.execute(i, indice, graphVisual); //串行版本（改进djd）

//                        double[] DJDoriginalresult = p.ejecutaAccion(i, indice, graphVisual); //串行版本（原始djd）
                Results DJDoriginalresult = p.execute(i, indice, false);
//                Results DJDoriginalresult = p.ejecutaAccion(i, indice, false);

                sheets = DJDoriginalresult.getListaObjetos();

                executionTime[indice][i] = DJDoriginalresult.getUsedtime();
                numObjects[indice][i] = DJDoriginalresult.getBinnum();
                aptitudes[indice][i] = DJDoriginalresult.getAptitude();
                Kvalues[indice][i] = DJDoriginalresult.getKvalue();
                utilizations[indice][i] = DJDoriginalresult.getUtilization();

                // new
                List<Sheet> finalBin = DJDoriginalresult.getListaObjetos();

                // 参数i就是启发式的编号
                //aptitudes[indice][i]=p.executeParallel(i, indice, graphVisual); //并行版本（未实现）
//                        numObjects[indice][i] = p.numeroObjetos();
//                        long stop = System.currentTimeMillis();
                // 结束计算，终止时间

//                        executionTime[indice][i] = (int) (stop - start);
            }

            indice++;
        }
        //结束

//            int[] indiceMejores = rw.buscarMejor(aptitudes, numObjects, indice, numHeuristicas);
//            try {
//                rw.instancesSolution(instancesSolution0, instancesSolution, problems, aptitudes, numObjects, executionTime, indiceMejores, indice);
//            } catch (Exception e) {
//                System.err.println("错误");
//                System.exit(0);
//            }
        return sheets;
    }

    public static void run(File dirAllInstance, File dirSolution, File filePath, File archieveProblems,
                           String instances, int numHeuristicas, boolean repeticion, boolean graphVisual) throws Exception {

        RWfiles rw = new RWfiles();
        List<String> problems = new ArrayList<String>();
        File instancesSolution0 = new File(dirSolution, "solution_" + instances);
        File instancesSolution = new File(dirSolution, "solution_" + instances);

        if (!instancesSolution.exists() || instancesSolution.length() == 0 || repeticion) {
            try {
                // 读取listOfInstances，problems为[fuLB.txt, fuMB.txt, ...]
                problems = rw.loadProblems(archieveProblems);

            } catch (Exception e) {
                System.err.println("读取listOfInstances错误：" + archieveProblems);
                System.exit(0);
            }

            int numproblems = problems.size();
            double[][] aptitudes = new double[numproblems][numHeuristicas];  //每个样例的F值
            int[][] numObjects = new int[numproblems][numHeuristicas];       //每个样例所用的底板数量
            int[][] executionTime = new int[numproblems][numHeuristicas];   //每个样例的执行时间
            double[][] utilizations = new double[numproblems][numHeuristicas]; // 每个样例的利用率
            double[][] Kvalues = new double[numproblems][numHeuristicas];  // 每个样例的K值

            int indice = 0;
            Iterator<String> iter = problems.iterator(); // problems的数量就是listOfInstances中问题的数量，人为设定
            while (iter.hasNext()) {
                Instance p = new Instance(numproblems * numHeuristicas);    //为每个样例及对应的方法生成一个instance求解
                String problem = iter.next();
                File instancesOut = new File(filePath, "solution_" + problem); //保存结果
                if (!instancesOut.exists() || instancesOut.length() == 0 || repeticion == true) {
                    PrintWriter printerWriter = new PrintWriter(instancesOut);
                    // 应用各种启发式求解
                    for (int i = 0; i < numHeuristicas; i++) {
                        System.out.println("Solving " + problem + " with heuristic " + i);

                        // 读取文件，分为txt和xlxs
                        if (problem.endsWith("txt")) {
                            p.obtainProblem(new File(dirAllInstance, problem));
                        } else { //excel文件
                            p.obtainProblemExcel(new File(dirAllInstance, problem));
                        }

                        // 开始计算，起始时间
                        long start = System.currentTimeMillis();
                        // TODO:这里控制采用哪种方法
//                        aptitudes[indice][i] = p.execute(i, indice, graphVisual); //串行版本（改进djd）

//                        double[] DJDoriginalresult = p.ejecutaAccion(i, indice, graphVisual); //串行版本（原始djd）
                        Results DJDoriginalresult = p.ejecutaAccion(i, indice, graphVisual);

                        executionTime[indice][i] = DJDoriginalresult.getUsedtime();
                        numObjects[indice][i] = DJDoriginalresult.getBinnum();
                        aptitudes[indice][i] = DJDoriginalresult.getAptitude();
                        Kvalues[indice][i] = DJDoriginalresult.getKvalue();
                        utilizations[indice][i] = DJDoriginalresult.getUtilization();

                        // new
                        List<Sheet> finalBin = DJDoriginalresult.getListaObjetos();

                        // 参数i就是启发式的编号
                        //aptitudes[indice][i]=p.executeParallel(i, indice, graphVisual); //并行版本（未实现）
//                        numObjects[indice][i] = p.numeroObjetos();
//                        long stop = System.currentTimeMillis();
                        // 结束计算，终止时间

//                        executionTime[indice][i] = (int) (stop - start);
                        printerWriter.println(i + "," + executionTime[indice][i] + "," + numObjects[indice][i] + "," +
                                aptitudes[indice][i] + "," + Kvalues[indice][i] + "," + utilizations[indice][i]);
                    }
                    printerWriter.close();

                } else {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(instancesOut));
                        String line = null;
                        String[] lineBreak;
                        for (int i = 0; i < numHeuristicas; i++) {
                            line = reader.readLine();
                            lineBreak = line.split(",");
                            aptitudes[indice][i] = Double.valueOf(lineBreak[1]);
                            numObjects[indice][i] = Integer.valueOf(lineBreak[2]);
                        }
                        reader.close();
                    } catch (Exception e) {
                        System.err.println("错误 : " + instancesOut);
                    }
                }
                indice++;
            }
            //结束

//            int[] indiceMejores = rw.buscarMejor(aptitudes, numObjects, indice, numHeuristicas);
//            try {
//                rw.instancesSolution(instancesSolution0, instancesSolution, problems, aptitudes, numObjects, executionTime, indiceMejores, indice);
//            } catch (Exception e) {
//                System.err.println("错误");
//                System.exit(0);
//            }
        }
    }

    public static List<List<Sheet>> run1(File dirAllInstance, File dirSolution, File filePath, File archieveProblems,
                                         String instances, int numHeuristicas, boolean repeticion, boolean graphVisual) throws Exception {

        List<List<Sheet>> sheets = new ArrayList<>();
        RWfiles rw = new RWfiles();
        List<String> problems = new ArrayList<String>();
        File instancesSolution0 = new File(dirSolution, "solution_" + instances);
        File instancesSolution = new File(dirSolution, "solution_" + instances);

        if (!instancesSolution.exists() || instancesSolution.length() == 0 || repeticion) {
            try {
                // 读取listOfInstances，problems为[fuLB.txt, fuMB.txt, ...]
                problems = rw.loadProblems(archieveProblems);

            } catch (Exception e) {
                System.err.println("读取listOfInstances错误：" + archieveProblems);
                System.exit(0);
            }

            int numproblems = problems.size();
            double[][] aptitudes = new double[numproblems][numHeuristicas];  //每个样例的F值
            int[][] numObjects = new int[numproblems][numHeuristicas];       //每个样例所用的底板数量
            int[][] executionTime = new int[numproblems][numHeuristicas];   //每个样例的执行时间
            double[][] utilizations = new double[numproblems][numHeuristicas]; // 每个样例的利用率
            double[][] Kvalues = new double[numproblems][numHeuristicas];  // 每个样例的K值

            int indice = 0;
            Iterator<String> iter = problems.iterator(); // problems的数量就是listOfInstances中问题的数量，人为设定
            while (iter.hasNext()) {
                Instance p = new Instance(numproblems * numHeuristicas);    //为每个样例及对应的方法生成一个instance求解
                String problem = iter.next();
                File instancesOut = new File(filePath, "solution_" + problem); //保存结果
                if (!instancesOut.exists() || instancesOut.length() == 0 || repeticion == true) {
                    PrintWriter printerWriter = new PrintWriter(instancesOut);
                    // 应用各种启发式求解
                    for (int i = 0; i < numHeuristicas; i++) {
                        System.out.println("Solving " + problem + " with heuristic " + i);

                        // 读取文件，分为txt和xlxs
                        if (problem.endsWith("txt")) {
                            p.obtainProblem(new File(dirAllInstance, problem));
                        } else { //excel文件
                            p.obtainProblemExcel(new File(dirAllInstance, problem));
                        }

                        // 开始计算，起始时间
                        long start = System.currentTimeMillis();
                        // TODO:这里控制采用哪种方法
//                        aptitudes[indice][i] = p.execute(i, indice, graphVisual); //串行版本（改进djd）

//                        double[] DJDoriginalresult = p.ejecutaAccion(i, indice, graphVisual); //串行版本（原始djd）
                        Results DJDoriginalresult = p.ejecutaAccion(i, indice, graphVisual);

                        System.out.println(DJDoriginalresult.getListaObjetos());
                        sheets.add(DJDoriginalresult.getListaObjetos());


                        executionTime[indice][i] = DJDoriginalresult.getUsedtime();
                        numObjects[indice][i] = DJDoriginalresult.getBinnum();
                        aptitudes[indice][i] = DJDoriginalresult.getAptitude();
                        Kvalues[indice][i] = DJDoriginalresult.getKvalue();
                        utilizations[indice][i] = DJDoriginalresult.getUtilization();

                        // new
                        List<Sheet> finalBin = DJDoriginalresult.getListaObjetos();

                        // 参数i就是启发式的编号
                        //aptitudes[indice][i]=p.executeParallel(i, indice, graphVisual); //并行版本（未实现）
//                        numObjects[indice][i] = p.numeroObjetos();
//                        long stop = System.currentTimeMillis();
                        // 结束计算，终止时间

//                        executionTime[indice][i] = (int) (stop - start);
                        printerWriter.println(i + "," + executionTime[indice][i] + "," + numObjects[indice][i] + "," +
                                aptitudes[indice][i] + "," + Kvalues[indice][i] + "," + utilizations[indice][i]);
                    }
                    printerWriter.close();

                } else {
                    try {
                        BufferedReader reader = new BufferedReader(new FileReader(instancesOut));
                        String line = null;
                        String[] lineBreak;
                        for (int i = 0; i < numHeuristicas; i++) {
                            line = reader.readLine();
                            lineBreak = line.split(",");
                            aptitudes[indice][i] = Double.valueOf(lineBreak[1]);
                            numObjects[indice][i] = Integer.valueOf(lineBreak[2]);
                        }
                        reader.close();
                    } catch (Exception e) {
                        System.err.println("错误 : " + instancesOut);
                    }
                }
                indice++;
            }
            //结束

//            int[] indiceMejores = rw.buscarMejor(aptitudes, numObjects, indice, numHeuristicas);
//            try {
//                rw.instancesSolution(instancesSolution0, instancesSolution, problems, aptitudes, numObjects, executionTime, indiceMejores, indice);
//            } catch (Exception e) {
//                System.err.println("错误");
//                System.exit(0);
//            }
        }
        return sheets;
    }
}
