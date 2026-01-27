package com.example.demo.controller;

import com.example.demo.demos.web.User;
import com.example.demo.share.RunHeuristics;
import com.example.demo.share.Sheet;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@RequestMapping("/test")
@RestController
@CrossOrigin
public class TestController {
//    http://127.0.0.1:8090/test/list
    @GetMapping("/list")
    public List<User> getAllUsers(){
        List<User> userList = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            User user = new User();
            user.setName("user"+i);
            user.setAge(18+i);
            userList.add(user);
        }
        return userList;
    }

    @GetMapping("/results")
    public List<List<Sheet>> getResults() throws Exception {
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
        List<List<Sheet>> lists = RunHeuristics.run1(dirAllInstance, dirSolution, filePath, archieveProblems,
                instances, numHeuristics, repetition, graphVisual);
        System.out.println("Finish");
        System.out.println();
        return lists;
    }
}
