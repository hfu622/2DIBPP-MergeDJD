package com.example.demo.share;

import com.example.demo.utils.InputData;
import com.example.demo.utils.Points;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Instance {
    private List<Sheet> listaObjetos = new LinkedList<Sheet>();
    List<Piece> listapiezas = new LinkedList<Piece>();        //零件列表
    List<Piece> listapiezasFijas = new LinkedList<Piece>();    //排列
    private int xObjeto, yObjeto;                            //Object size
    private int numpiezas;                                    //零件个数，循环读取实例用
    private int noPzasAcomodar;                            //零件个数，没啥用
    private int Totalpiezas;                                //零件的总面积，没啥用
    public ResultVisual[] nuevo;                            //可视化结果
    private List<Vector<Sheet>> visualList = new ArrayList<>(); //所有可视化结果


    // 重分配器
    public RePlacement rePlacement = new RePlacement();

    public Instance(int indi) {
        nuevo = new ResultVisual[indi];
    }

    public void obtainProblemInfo(InputData data) {
        Totalpiezas = 0;
        numpiezas = data.getNum();
        List<List<Points>> pieces = data.getPieces();
        for (int i = 0; i < pieces.size(); i++) {
            List<Points> piece = pieces.get(i);
            int[] vertices = new int[piece.size() * 2];
            for (int j = 0; j < piece.size(); j++) {
                vertices[j * 2] = piece.get(j).getX();
                vertices[j * 2 + 1] = piece.get(j).getY();
            }
            Piece piece1 = new Piece(vertices);
            Totalpiezas += piece1.getTotalSize();
            listapiezas.add(piece1);
            listapiezasFijas.add(piece1);
        }
        if (!listaObjetos.isEmpty())
            listaObjetos.clear();
        int width = data.getWidth();
        int height = data.getHeight();
        listaObjetos.add(new Sheet(width, height, 0));
        xObjeto = width;
        yObjeto = height;
        noPzasAcomodar = (int) (listapiezas.size());
        System.out.println("Pieces to place: " + noPzasAcomodar + " into objects of size " + xObjeto + " x " + yObjeto);
    }

    /**
     * 获取txt问题实例
     */
    public void obtainProblem(File file) {
        RWfiles rw = new RWfiles();
        int[][] matriz = null;
        try {
            matriz = rw.obtainMatriz(file, 37);  //把文件封装到矩阵里
        } catch (Exception e) {
            System.err.println("读取文件错误 : " + file);
        }

        Totalpiezas = 0;
        numpiezas = matriz[0][0];

        //Pone las piezas en el arreglo de piezas no acomodadas
        for (int m = 0; m < numpiezas; m++) {
            int numLados = matriz[0][m + 2];
            int[] vertices = new int[numLados * 2];
            for (int i = 0; i < numLados * 2; i += 2) {
                vertices[i] = matriz[i + 1][m + 2];
                vertices[i + 1] = matriz[i + 2][m + 2];
            }
            Piece piece = new Piece(vertices);
            piece.setnumber(m);
            this.Totalpiezas += piece.getTotalSize();
            this.listapiezas.add(piece);
            this.listapiezasFijas.add(piece);
        }

        if (listaObjetos.size() > 0)
            listaObjetos.clear();
        listaObjetos.add(new Sheet(matriz[0][1], matriz[1][1], 0));
        xObjeto = matriz[0][1];
        yObjeto = matriz[1][1];
        noPzasAcomodar = (int) (listapiezas.size());
        System.out.println("Pieces to place: " + noPzasAcomodar + " into objects of size " + xObjeto + " x " + yObjeto);
    }

    // 读取excel问题实例
    public void obtainProblemExcel(File file) {
        try {
            Workbook workbook = Workbook.getWorkbook(file);
            jxl.Sheet sheet = workbook.getSheet(0);
            numpiezas = sheet.getRows() - 1; //零件个数，最后一行为长宽
            int k = 0;
            for (int i = 0; i < numpiezas; i = i + 2) { //第i行
                int len = sheet.getRow(i).length;//获取第i行的cell

                // 顶点
                List<Integer> list = new ArrayList<>(); //保存顶点
                int numofVertices = 0;
                for (int j = 0; j < len; j++) {
                    if (sheet.getCell(j, i).getContents().isEmpty()) {
                        break;
                    }
                    numofVertices++;
                    int x = Integer.valueOf(sheet.getCell(j, i).getContents());
                    int y = Integer.valueOf(sheet.getCell(j, i + 1).getContents());
                    list.add(x);
                    list.add(y);
                }
                if (list.size() > 0) {
                    Integer[] vertices = list.toArray(new Integer[numofVertices * 2]);

                    Piece piece = new Piece(vertices);
                    piece.setnumber(k++);
                    this.Totalpiezas += piece.getTotalSize();
                    this.listapiezas.add(piece);
                }
            }
            // 获取底板长宽
            xObjeto = Integer.valueOf(sheet.getCell(0, numpiezas).getContents());
            yObjeto = Integer.valueOf(sheet.getCell(1, numpiezas).getContents());

        } catch (IOException e) {
            e.printStackTrace();
        } catch (BiffException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param action
     * @param indi
     * @param graphVisual
     * @return
     */
    public Results execute(int action, int indi, boolean graphVisual) {
        /**
         * 这里加禁忌搜索框架，中间要返回结合结果，对结果最差（这里建议改为轮盘赌）的进行破坏重新结合，如果结果都好，比如为1，不破坏
         * 每一轮ban掉合并效果最差的，可以是最初两两合并，也可以是中间的合并过程，也就是说，禁忌表保存的是每一次合并的过程
         * 用个set就可以维护了，set的元素是排序了的合并零件id，并且加上一些分隔符，或者是用个队列
         */
        // 禁忌列表和阈值
//        //Set<String> tabuSet = new HashSet<>(3); 		//保存合并过程的禁忌表，容量为3
//        Queue<String> tabuQueue = new LinkedList<>();    //用队列来保存禁忌表，容量为5
//        double initThreshold = 2;                        //合并后有个得分，超过初始可接受阈值就能合并，改成传给replacement的成员变量了
//        int tabuSize = 3;                                //禁忌列表长度

        Results result = new Results();

        // 原始的零件列表，要存着
        List<Piece> originPieceList = new ArrayList<>();
        for (Piece piece : listapiezas) {                //保存最原始的零件列表
            originPieceList.add(piece);
        }

        // 区分凹凸多边形，先调用这一步效果更差了，不知为啥
        // 在 combineNonConvex() 函数中调用
//        rePlacement.findConvexAndNonconvex(originPieceList, rePlacement.convexList, rePlacement.nonConvexList);

        long startTime = System.currentTimeMillis();
        List<Double> resultList = new ArrayList<>();    //中间结果列表
        double aptitud = 0;                            //最佳结果
        rePlacement.threshold = 2;                        //最大阈值
//        rePlacement.smallThreshold = 1;                //最低阈值，降到此就不会再降了
//        rePlacement.tabuQueue = tabuQueue;
        // TODO：将placement和matching分开
        rePlacement.setOriginPieceList(originPieceList);

        //  结合，这里还要返回结合的结果
        rePlacement.processUion.clear(); //清空一下合并结果
        rePlacement.xObject = xObjeto;
        rePlacement.yObject = yObjeto;
        listapiezas = rePlacement.combineNonConvex();

        // 3.取出结合的结果，按fitness从大到小排序
        List<Map<String, Double>> processUnion = rePlacement.processUion;
//            Collections.sort(processUnion, new Comparator<Map<String, Double>>() {
//                @Override
//                public int compare(Map<String, Double> o1, Map<String, Double> o2) {
//                    Double f1 = 0d;
//                    Double f2 = 0d;
//                    for (Map.Entry<String, Double> entry : o1.entrySet()) {
//                        f1 = entry.getValue();
//                    }
//                    for (Map.Entry<String, Double> entry : o2.entrySet()) {
//                        f2 = entry.getValue();
//                    }
//                    if (f1 - f2 < 0) {
//                        return -1;
//                    } else {
//                        return 1;
//                    }
//                }
//            });

        // TODO:这里改过了
        Collections.sort(processUnion, new Comparator<Map<String, Double>>() {
            @Override
            public int compare(Map<String, Double> o1, Map<String, Double> o2) {
                Double f1 = 0d;
                Double f2 = 0d;
                for (Map.Entry<String, Double> entry : o1.entrySet()) {
                    f1 = entry.getValue();
                }
                for (Map.Entry<String, Double> entry : o2.entrySet()) {
                    f2 = entry.getValue();
                }
                return f1.compareTo(f2);
            }
        });

        // 首先确保合并表不是空的，todo:如果是空的就直接套料?
//        if (!processUnion.isEmpty()) {
//            // 获得fitness最小的那个，加入禁忌表
//            String fit = (String) processUnion.get(0).keySet().stream().toArray()[0];
//            // 如果禁忌表的长度小于3，且不包含即将加入的元素，才加入
//            if (queueContains(tabuQueue, fit)) { //如果存在，则随机选一个加入
//                // 随机选一个合并过程中的结果加入
//                int size = processUnion.size();
//                int randomNum = (int) (Math.random() * size);
//                fit = (String) processUnion.get(randomNum).keySet().stream().toArray()[0];
//                while (tabuQueue.contains(fit)) { //如果还重复了，再生成
//                    randomNum = (int) (Math.random() * size);
//                    fit = (String) processUnion.get(randomNum).keySet().stream().toArray()[0];
//                }
//                tabuQueue.offer(fit);
//            } else {
//                tabuQueue.offer(fit); //不存在才加入
//            }
//        }


        // 开始套料
        // 准备好零件和底板
        if (!listaObjetos.isEmpty())
            listaObjetos.clear();
        listaObjetos.add(new Sheet(xObjeto, yObjeto, 0));

        double currentResult; //当前结果
        ControlHeuristics control = new ControlHeuristics();
        do {
            control.executeHeuristic(listapiezas, listaObjetos, 0);
        } while (!listapiezas.isEmpty());

        // 6.计算结果，清除没有零件的底板
        for (int i = 0; i < listaObjetos.size(); i++) {
            Sheet objk = (Sheet) listaObjetos.get(i);
            List<Piece> Lista2 = objk.getPzasInside();
            if (Lista2.size() == 0)
                listaObjetos.remove(i);
        }

        long endTime = System.currentTimeMillis();

        aptitud = control.calcularAptitud(listaObjetos);
        int ax = (int) (aptitud * 1000.0);
        aptitud = (double) ax / 1000.0;
        double utilization = control.calculateUtilization(listaObjetos);
        double Kvalue = control.calculateK(listaObjetos);
        int binnum = listaObjetos.size();
        System.out.println("Bin number:" + binnum);
        System.out.println("F value:" + aptitud);
        System.out.println("Utilization:" + utilization);
        System.out.println("K value:" + Kvalue);

        result.setUsedtime((int) (endTime - startTime));
        result.setListaObjetos(listaObjetos);
        result.setBinnum(binnum);
        result.setAptitude(aptitud);
        result.setUtilization(utilization);
        result.setKvalue(Kvalue);

        // 7.还原结果（后加的）
        for (int i = 0; i < listaObjetos.size(); i++) {
            // 对每个底板上的零件
            Sheet sheet = listaObjetos.get(i);
            List<Piece> pieceList = sheet.getPzasInside();

            boolean flag = true;
            while (flag) {
                boolean happen = false; //发生了删除
                for (int j = 0; j < pieceList.size(); j++) {
                    Piece piece = pieceList.get(j);
                    if (piece.child.size() > 0) {
                        double rotate = piece.getRotada();
                        piece.rotateCori(rotate); //旋转原坐标，再计算移动了多少
                        int shifx = piece.coordX[0] - piece.getCoriX()[0]; //x坐标的移动长度
                        int shify = piece.coordY[0] - piece.getCoriY()[0]; //y坐标的移动长度
                        movereStore(piece, rotate, shifx, shify, pieceList); //将孩子节点都加入当前底板的零件列表
                        pieceList.remove(piece); //删掉这个父节点
                        happen = true;
                        break; //只有还有孩子节点的零件就会一直重复
                    }
                }
                if (happen) { //有删除，还得继续循环
                    flag = true;
                } else {
                    flag = false;
                }
            }
        }

        // 这里可视化，暂时用不上
        // 8.中间结果可视化
        Vector<Sheet> listita = new Vector<Sheet>();
        for (int i = 0; i < listaObjetos.size(); i++) {
            listita.add((Sheet) (listaObjetos.get(i)));
        }
        if (graphVisual) {
            nuevo[indi] = new ResultVisual(listita);
            nuevo[indi].setSize(700, 650);
            nuevo[indi].setVisible(true);
        }
        visualList.add(listita);


//        long endTime = System.currentTimeMillis();
//        System.out.println("执行时间：" + (endTime - startTime) + "ms");
//
//        // 9.再跑一遍不结合的
//        bestResult = getBestResult(indi, graphVisual, originPieceList, resultList, bestResult);
//
//        System.out.println("process：" + resultList);
//        System.out.println("best result：" + bestResult);

        return result;
    }


    /**
     * @param action      并行版本
     * @param indi
     * @param graphVisual
     * @return
     */
    public double executeParallel(int action, int indi, boolean graphVisual) {
        // 开个线程池
        int nThreads = 4;
        CountDownLatch countDownLatch = new CountDownLatch(nThreads);
        ExecutorService executorService = Executors.newFixedThreadPool(nThreads);
        Double[] results = new Double[nThreads];
        // 赋值四份零件列表
        List<List<Piece>> parallelPieceList = new ArrayList<>();
        for (int j = 0; j < nThreads; j++) {
            // 赋值零件列表
            List<Piece> pieceList = new LinkedList<>();
            for (Piece piece : listapiezas) {
                try {
                    pieceList.add((Piece) piece.clone());
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            parallelPieceList.add(pieceList);
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 开始并行计算
        for (int i = 0; i < nThreads; i++) {
            int id = i;
            executorService.submit(() -> {
                System.out.println("current Thread:" + Thread.currentThread().getName() + " begin");
                // 记录每个线程运行得到的最大值。
                results[id] = singleThreadTask(indi, graphVisual, parallelPieceList.get(id));
                countDownLatch.countDown();
                System.out.println("current Thread:" + Thread.currentThread().getName() + " countdown");
            });
        }
        // 等待所有任务结束
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // 返回每个线程处理结果的最大值
//		return Arrays.stream(results).max((d1, d2)->{
//			return d1.compareTo(d2);
//		}).get();
        executorService.shutdown();
        return Arrays.stream(results).max(Double::compareTo).get();
    }

    /**
     * 每个线程的任务
     */
    // 没用到
    private double singleThreadTask(int indi, boolean graphVisual, List<Piece> originPieceList) {
        // 禁忌列表和阈值
        Queue<String> tabuQueue = new LinkedList<>();    //用队列来保存禁忌表，容量为5
        double initThreshold = 2;                        //合并后有个得分，超过初始可接受阈值就能合并，改成传给replacement的成员变量了
        int tabuSize = 5;                                //禁忌列表长度

        // 区分凹凸多边形，先调用这一步效果更差了，不知为啥
        RePlacement rePlacement = new RePlacement();
        rePlacement.findConvexAndNonconvex(originPieceList, rePlacement.convexList, rePlacement.nonConvexList);


        // 禁忌搜索开始
        List<Double> resultList = new ArrayList<>();    //中间结果列表
        double bestResult = 0;                            //最佳结果
        rePlacement.threshold = 2;                        //最大阈值
        rePlacement.smallThreshold = 0.4;                //最低阈值，降到此就不会再降了
        rePlacement.tabuQueue = tabuQueue;
        int iteration = 1;                                //运行代数
        bestResult = tabuSearch(iteration, indi, graphVisual, tabuQueue, initThreshold, tabuSize, originPieceList, resultList, bestResult, rePlacement);

        // 再跑一遍不结合的
        //bestResult = getBestResult(indi, graphVisual, originPieceList, resultList, bestResult);

        System.out.println("current Thread: " + Thread.currentThread().getName() + ".  process:" + resultList + ".  best result:" + bestResult);

        return bestResult;
    }


    private double tabuSearch(int iteration, int indi, boolean graphVisual, Queue<String> tabuQueue, double initThreshold, int tabuSize, List<Piece> originPieceList, List<Double> resultList, double bestResult, RePlacement rePlacement) {
        List<Piece> pieceListParallel = new LinkedList<>();
        for (int it = 0; it < iteration; it++) {
            // 0. 原始零件赋值
            rePlacement.setOriginPieceList(originPieceList);
            // 1. 设置fitness的接受阈值
            rePlacement.threshold -= it * 0.1; //阈值逐渐减小，直到最低可接受阈值n？
            if (rePlacement.threshold < rePlacement.smallThreshold) {
                // 降低后是保持在低阈值还是重来？
                //rePlacement.threshold = rePlacement.smallThreshold;
                rePlacement.threshold = initThreshold;
            }
            // 2. 结合，这里还要返回结合的结果
            rePlacement.processUion.clear(); //清空一下合并结果
            rePlacement.xObject = xObjeto;
            rePlacement.yObject = yObjeto;
            pieceListParallel = rePlacement.combineNonConvex();


            // 3.取出结合的结果，按fitness从大到小排序
            List<Map<String, Double>> processUnion = rePlacement.processUion;
            Collections.sort(processUnion, new Comparator<Map<String, Double>>() {
                @Override
                public int compare(Map<String, Double> o1, Map<String, Double> o2) {
                    Double f1 = 0d;
                    Double f2 = 0d;
                    for (Map.Entry<String, Double> entry : o1.entrySet()) {
                        f1 = entry.getValue();
                    }
                    for (Map.Entry<String, Double> entry : o2.entrySet()) {
                        f2 = entry.getValue();
                    }
                    if (f1 - f2 < 0) {
                        return -1;
                    } else {
                        return 1;
                    }
                }
            });


            // 4.更新禁忌表，三轮后就更新吗，因为如果一直重复，那queue就一直不更新
            if (it % tabuSize == 0) {
                tabuQueue.poll(); //弹出保存最久的
            }
            // 首先确保合并表不是空的，todo:如果是空的就直接套料?
            if (!processUnion.isEmpty()) {
                // 获得fitness最小的那个，加入禁忌表
                String fit = (String) processUnion.get(0).keySet().stream().toArray()[0];
                // 如果禁忌表的长度小于3，且不包含即将加入的元素，才加入
                if (queueContains(tabuQueue, fit)) { //如果存在，则随机选一个加入
                    // 随机选一个合并过程中的结果加入
                    int size = processUnion.size();
                    int randomNum = (int) (Math.random() * size);
                    fit = (String) processUnion.get(randomNum).keySet().stream().toArray()[0];
                    while (tabuQueue.contains(fit)) { //如果还重复了，再生成
                        randomNum = (int) (Math.random() * size);
                        fit = (String) processUnion.get(randomNum).keySet().stream().toArray()[0];
                    }
                    tabuQueue.offer(fit);
                } else {
                    tabuQueue.offer(fit); //不存在才加入
                }
            }


            // 5.开始套料
            // 准备好零件和底板
            List<Sheet> listaObjetos = new LinkedList<>();
            if (listaObjetos.size() > 0)
                listaObjetos.clear(); //Limpiar el contenedor
            listaObjetos.add(new Sheet(xObjeto, yObjeto, 0));

            double currentResult; //当前结果
            ControlHeuristics control = new ControlHeuristics();
            do {
                control.executeHeuristic(pieceListParallel, listaObjetos, 0);
            } while (pieceListParallel.size() > 0);


            // 6.计算结果，清除没有零件的底板
            for (int i = 0; i < listaObjetos.size(); i++) {
                Sheet objk = (Sheet) listaObjetos.get(i);
                List<Piece> Lista2 = objk.getPzasInside();
                if (Lista2.size() == 0)
                    listaObjetos.remove(i);
            }
            // 计算结果
            currentResult = control.calcularAptitud(listaObjetos);
            int ax = (int) (currentResult * 1000.0);
            currentResult = (double) ax / 1000.0;
            resultList.add(currentResult);
            // 更新最佳结果
            if (currentResult > bestResult) {
                bestResult = currentResult;
            }


            // 7.还原结果（后加的）
            for (int i = 0; i < listaObjetos.size(); i++) {
                // 对每个底板上的零件
                Sheet sheet = listaObjetos.get(i);
                List<Piece> pieceList = sheet.getPzasInside();

                boolean flag = true;
                while (flag) {
                    boolean happen = false; //发生了删除
                    for (int j = 0; j < pieceList.size(); j++) {
                        Piece piece = pieceList.get(j);
                        if (piece.child.size() > 0) {
                            double rotate = piece.getRotada();
                            piece.rotateCori(rotate); //旋转原坐标，再计算移动了多少
                            int shifx = piece.coordX[0] - piece.getCoriX()[0]; //x坐标的移动长度
                            int shify = piece.coordY[0] - piece.getCoriY()[0]; //y坐标的移动长度
                            movereStore(piece, rotate, shifx, shify, pieceList); //将孩子节点都加入当前底板的零件列表
                            pieceList.remove(piece); //删掉这个父节点
                            happen = true;
                            break; //只有还有孩子节点的零件就会一直重复
                        }
                    }
                    if (happen) { //有删除，还得继续循环
                        flag = true;
                    } else {
                        flag = false;
                    }
                }
            }


            // 8.可视化结果
            //Get graph of the results:
            ResultVisual[] nuevo = new ResultVisual[4];
            if (graphVisual) {
                Vector<Sheet> listita = new Vector<Sheet>();
                for (int i = 0; i < listaObjetos.size(); i++) {
                    listita.add((Sheet) (listaObjetos.get(i)));
                }
                nuevo[indi] = new ResultVisual(listita);
                nuevo[indi].setSize(700, 650);
                nuevo[indi].setVisible(true);
            }

        }
        return bestResult;
    }

    private double getBestResult(int indi, boolean graphVisual, List<Piece> originPieceList, List<Double> resultList, double bestResult) {
        if (listaObjetos.size() > 0)
            listaObjetos.clear(); //Limpiar el contenedor
        listaObjetos.add(new Sheet(xObjeto, yObjeto, 0));
        // 如果要尝试一次结合的
        //rePlacement.setOriginPieceList(originPieceList);
        //listapiezas = rePlacement.combineNonConvex();
        // 将零件赋值到listapiezas里面
        for (Piece piece : originPieceList) {
            listapiezas.add(piece);
        }

        double aptitud; //action?0??FF-BL??3??DJD-MA
        ControlHeuristics control = new ControlHeuristics();
        do {
            control.executeHeuristic(listapiezas, listaObjetos, 0);
        } while (listapiezas.size() > 0);

        for (int i = 0; i < listaObjetos.size(); i++) {
            Sheet objk = (Sheet) listaObjetos.get(i);
            List<Piece> Lista2 = objk.getPzasInside();
            if (Lista2.size() == 0)
                listaObjetos.remove(i);
        }

        // 计算结果
        aptitud = control.calcularAptitud(listaObjetos);
        int ax = (int) (aptitud * 1000.0);
        aptitud = (double) ax / 1000.0;
        resultList.add(aptitud); //添加到结果列表
        // 更新最佳结果
        if (aptitud > bestResult) {
            bestResult = aptitud;
        }

        //中间结果可视化:
//        Vector<Sheet> listita = new Vector<Sheet>();
//        for (int i = 0; i < listaObjetos.size(); i++) {
//            listita.add((Sheet) (listaObjetos.get(i)));
//        }
//        if (graphVisual) {
//            nuevo[indi] = new ResultVisual(listita);
//            nuevo[indi].setSize(700, 650);
//            nuevo[indi].setVisible(true);
//        }
//        visualList.add(listita);
        return bestResult;
    }

    /**
     * 将piece的孩子结点都加入piecelist，并删掉每个孩子结点的孩子结点
     */
    private void movereStore(Piece piece, double rotate, int shifx, int shify, List<Piece> pieceList) {
        if (piece.child.size() > 0) {
            movereStore(piece.child.get(0), rotate, shifx, shify, pieceList);
            movereStore(piece.child.get(1), rotate, shifx, shify, pieceList);
            //piece.child.clear(); //孩子结点加完后，就删掉
        }

        if (piece.child.size() == 0) {
            // 先旋转
            piece.rotate(rotate);
            // x,y坐标分别移动
            for (int i = 0; i < piece.coordX.length; i++) {
                piece.coordX[i] += shifx;
                piece.coordY[i] += shify;
            }
            pieceList.add(piece);
        }

    }


    /**
     * 只给出利用率不满的底板的重排样结果
     */
    private void reExecute(List<Piece> listapiezas, List<Sheet> listaObjetos, int action) {
        listapiezas.clear();
        // 保存零件以及已经装满的底板
        List<Sheet> fullObjectList = new ArrayList<>();
        for (int i = 0; i < listaObjetos.size(); i++) {
            // 都没有装满的情况下，最大的跳过
            if (i == listaObjetos.size() - 1) {
                fullObjectList.add(listaObjetos.get(i));
                continue;
            }
            // 装满的跳过
            if (listaObjetos.get(i).getFreeArea() == 0) {
                fullObjectList.add(listaObjetos.get(i));
                continue;
            }
            List<Piece> pzasInside = listaObjetos.get(i).getPzasInside();
            for (int j = 0; j < pzasInside.size(); j++) {
                listapiezas.add(pzasInside.get(j));
            }
        }
        // 清空底板
        listaObjetos.clear();
        listaObjetos.add(new Sheet(1000, 1000, 0));
        // 重新排版
        ControlHeuristics control = new ControlHeuristics();
        do {
            control.executeHeuristic(listapiezas, listaObjetos, action);
        } while (listapiezas.size() > 0);

        // 将满的底板重新加入
        for (int i = 0; i < fullObjectList.size(); i++) {
            listaObjetos.add(fullObjectList.get(i));
        }
    }


    public int numeroObjetos() {
        return listaObjetos.size();
    }

    // 移动所有零件至左下角
    public void moveAllPieceToBottomLeft(List<Piece> listapiezas) {
        for (int i = 0; i < listapiezas.size(); i++) {
            Piece piece = listapiezas.get(i);
            piece.moveToXY(0, 0, 2);
            if (piece.child.size() != 0) {
                for (int j = 0; j < piece.child.size(); j++) {
                    piece.child.get(j).moveToXY(0, 0, 2);
                }
            }
        }
    }

    // 查看队列里是否含有元素
    public boolean queueContains(Queue<String> queue, String str) {
        for (String s : queue) {
            if (s.equals(str)) {
                return true;
            }
        }
        return false;
    }

	// 主要的执行阶段
    // TODO: 当前的ejecutaAccion没有用到tabu和merge
    public Results ejecutaAccion(int action, int indi, boolean graficar) {
        /* 要计算执行时间的代码段 */
        long startTime = System.currentTimeMillis();
//        double[] result = new double[5];
        Results result = new Results();

        if (!listaObjetos.isEmpty())
            listaObjetos.clear();
        listaObjetos.add(new Sheet(xObjeto, yObjeto, 0));
        double aptitud; //action?0??FF-BL??3??DJD-MA
        ControlHeuristics control = new ControlHeuristics();
        do {
            control.executeHeuristic(listapiezas, listaObjetos, action);
        } while (!listapiezas.isEmpty());
        /**
         * ?replacement
         */

        long endTime = System.currentTimeMillis();
        System.out.println("执行时间：" + (endTime - startTime) + "ms");

        // delete the bin that cannot pack any item ?????item?bin
        for (int i = 0; i < listaObjetos.size(); i++) {
            Sheet objk = (Sheet) listaObjetos.get(i);
            List<Piece> Lista2 = objk.getPzasInside();
            if (Lista2.size() == 0)
                listaObjetos.remove(i);
        }

//        moveAllPieceToBottomLeft(listaObjetos.get(listaObjetos.size() - 1).getPzasInside());

        aptitud = control.calcularAptitud(listaObjetos);
        int ax = (int) (aptitud * 1000.0);
        aptitud = (double) ax / 1000.0;
        double utilization = control.calculateUtilization(listaObjetos);
        double Kvalue = control.calculateK(listaObjetos);
        int binnum = listaObjetos.size();
        System.out.println("Bin number:" + binnum);
        System.out.println("F value:" + aptitud);
        System.out.println("Utilization:" + utilization);
        System.out.println("K value:" + Kvalue);

//        result[0] = endTime - startTime;
//        result[1] = binnum;
//        result[2] = aptitud;
//        result[3] = Kvalue;
//        result[4] = utilization;
        result.setUsedtime((int)(endTime - startTime));
        result.setListaObjetos(listaObjetos);
        result.setBinnum(binnum);
        result.setAptitude(aptitud);
        result.setUtilization(utilization);
        result.setKvalue(Kvalue);

        // NOTE:暂时不用可视化
        //Get graph of the results:
//		if (graficar){
//			Vector<Sheet> listita = new Vector<Sheet>();
//			for(int i=0; i<listaObjetos.size(); i++)
//			{
//				listita.add((Sheet)(listaObjetos.get(i)));
//			}
//			nuevo[indi] = new ResultVisual(listita);
//			nuevo[indi].setSize(700, 650);
//			nuevo[indi].setVisible(true);
//		}
        return result;
    }
}

class Results{
    private List<Sheet> listaObjetos = new LinkedList<Sheet>();
    private int usedtime;
    private double aptitude;
    private double utilization;
    private double Kvalue;
    private int binnum;

    public Results(List<Sheet> listaObjetos, int usedtime, double aptitude, double utilization, double Kvalue, int binnum) {
        this.listaObjetos = listaObjetos;
        this.usedtime = usedtime;
        this.aptitude = aptitude;
        this.utilization = utilization;
        this.Kvalue = Kvalue;
        this.binnum = binnum;
    }

    public Results() {
    }

    public void setListaObjetos(List<Sheet> listaObjetos) {
        this.listaObjetos = listaObjetos;
    }
    public List<Sheet> getListaObjetos() {
        return listaObjetos;
    }
    public void setUsedtime(int usedtime) {
        this.usedtime = usedtime;
    }
    public int getUsedtime() {
        return usedtime;
    }
    public void setAptitude(double aptitude) {
        this.aptitude = aptitude;
    }
    public double getAptitude() {
        return aptitude;
    }
    public void setUtilization(double utilization) {
        this.utilization = utilization;
    }
    public double getUtilization() {
        return utilization;
    }
    public void setKvalue(double Kvalue) {
        this.Kvalue = Kvalue;
    }
    public double getKvalue() {
        return Kvalue;
    }
    public void setBinnum(int binnum) {
        this.binnum = binnum;
    }
    public int getBinnum() {
        return binnum;
    }

}