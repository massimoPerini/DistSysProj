package supervisor.communication;

import operator.types.Sum;
import org.jetbrains.annotations.NotNull;
import supervisor.communication.message.HeartbeatRequest;
import supervisor.communication.message.OperatorDeployment;

import java.net.Socket;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by massimo on 10/02/18.
 */
public class NodeSocket {

    private final TaskSocket daemonSocket;
    private final List<TaskSocket> operatorsSocket;
    private final ExecutorService executorService;
    private static final int DELAY = 5000;

    NodeSocket(@NotNull TaskSocket daemonSocket)
    {
        this.daemonSocket = daemonSocket;
        this.operatorsSocket = Collections.synchronizedList(new ArrayList<TaskSocket>());
        this.executorService = Executors.newCachedThreadPool();
    }

    public void addOperatorSocket(@NotNull TaskSocket taskSocket)
    {
        operatorsSocket.add(taskSocket);
    }

    public void doHearbeat()
    {
        Timer [] timers = new Timer [operatorsSocket.size()+1];

        for (int i=0;i<timers.length;i++)
        {
            timers[i] = new Timer(true);
            timers[i].schedule(new TimerTask() {
                @Override
                public void run()
                {
                    System.out.println("TIMER!!!!!!!!");
                }
            }, DELAY);

            if (i==0){
                executorService.submit(daemonSocket::listen);
            }
            else
            {
                executorService.submit(operatorsSocket.get(i-1)::listen);
            }
        }

        daemonSocket.send(new HeartbeatRequest(0));

        for (int i=0;i<operatorsSocket.size();i++)
        {
            operatorsSocket.get(i).send(new HeartbeatRequest(i+1));
        }
    }

    void deployOperator(OperatorDeployment operatorDeployment)
    {
        daemonSocket.send(operatorDeployment);
    }


}
