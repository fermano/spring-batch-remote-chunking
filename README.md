# Scaling jobs with Spring Batch Remote Chunking #

Recently in IT company where I work, I was asked to **add Spring Batch Remote Chunking support** into our system. So I just want to share my experience with that, make a demo and give some recommendations. Because no one on the internet was actually able to explain remote chunking in detail. First just briefly, Spring Batch Remote Chunking is a way of making **Spring Batch jobs running in parallel at N remote machines concurrently**. Sounds good? Allright, let's get to business. What do we need for it? First of all middleware for publishing chunks to remote machines. Almost everyone is using **JMS** for that, using this approach **you need to define two JMS queues**, one for **publishing chunk requests** to slaves and one for getting replies from slaves about **how they processed that particular chunk**. 

## Spring components necessary to define at Master Node  ##

### ChunkMessageChannelItemWriter ###

For example:

```
    <bean id="chunkWriter" class="org.springframework.batch.integration.chunk.ChunkMessageChannelItemWriter" scope="step">
        <property name="messagingOperations" ref="messagingGateway" />
        <property name="replyChannel" ref="masterChunkReplies" />
        <property name="throttleLimit" value="100" />
        <property name="maxWaitTimeouts" value="3000" />
    </bean>
```

This bean is in duty of publishing ChunkRequests to slaves(**messagingOperations** parameter) and getting the replies from slaves (**replyChannel** parameter). Basic idea of this class is that there is a class in it called **LocalState** which has two atomic integers called **expected** and **actual**. Expected is incremented **per sended ChunkRequest** to slave and actual is incremented **per received ChunkResponse**. So substract of 

```
LocalState.Expected - LocalState.Actual = how many remote slaves are currently processing the chunks. 
```

Every now and then we need to wait for them to finish. This is the moment when parameter **throttleLimit** comes in, it says *what is the maximal count of chunk processing receivers we are going to be waiting on before next ChunkRequests will be published again*. It is to avoid the overwhelming the receivers. At the end of a job, when all chunks are published, we need to wait for all results from slaves, how many times we will ask the replyChannel for replies is saved in "**maxWaitTimeouts**" parameter.

### Recommendations ###

If you've got really *fast reader of remotely chunked step, then set the throttleLimit to relatively high value, because you don't want to block chunkRequests publishing too often*. Setting the maxWaitTimeouts parameter *should depends on how fast are your slaves with chunks*. If they're handling them fast then set maxWaitTimeouts to just a little bit higher value then throttleLimit.

### Publishing ChunkRequests and getting ChunkResponses(messagingGateway, masterChunkReplies params)  ###

To send ChunkRequests from master node, you need to define messagingGateway for publishing it. 
You can do it for example like this:

```
    <bean id="messagingGateway" class="org.springframework.integration.core.MessagingTemplate">
        <property name="defaultChannel" ref="masterChunkRequests" />
        <property name="receiveTimeout" value="2000" />
    </bean>


    <!-- Move chunks to subscribers in parallel, with poolExecutor -->
    <int:channel id="masterChunkRequests" >
        <int:dispatcher task-executor="requestsPushingExecutor" />
    </int:channel>

    <int:channel id="masterChunkReplies" >
        <int:queue />
    </int:channel>

    <!-- Outbound channel adapter for sending requests (chunks) -->
    <jms:outbound-channel-adapter id="masterJMSRequests"
                                  channel="masterChunkRequests"
                                  connection-factory="remoteChunkingConnectionFactory"
                                  destination="remoteChunkingRequestsQueue"
            />

    <!-- Remote Chunking Replies From Slave -->
    <jms:message-driven-channel-adapter id="masterJMSReplies"
                                            connection-factory="remoteChunkingConnectionFactory"
                                            destination="remoteChunkingRepliesQueue"
                                            channel="masterChunkReplies"
                                            concurrent-consumers="10"
                                            max-concurrent-consumers="50"
                                            receive-timeout="5000"
                                            idle-task-execution-limit="10"
                                            idle-consumer-limit="5"
            />

    <int:logging-channel-adapter id="loggingChannel" level="INFO" log-full-message="true"/>

    <task:executor id="requestsPublishingExecutor" pool-size="10-50" queue-capacity="0" />
```

Let's explain previous configuration. First we defined direct channel called **masterChunkRequests** for publishing chunkRequests to slaves. If you've got really fast reader, like file reader, then I highly recommend to dispatch chunks in parallel, with task executor, like I did with **requestsPublishingExecutor**. You'll make chunks dispatching really fast then. Bean which performs own chunk dispatching is a simple JMS outbound-channel-adapter, see **masterJMSRequests**. Channel for getting replies from slaves needs to be **pollable channel**, see **masterChunkReplies**. 

### Recommendations ###

I highly recommend to **get ChunkResponses from slaves via message-driven-channel-adapter(see masterJMSReplies)** and **not via pooling** like you can find in the examples at github. Why? Because sometimes when slaves are really fast with processing the chunks I came into situation that I've got not consumed ChunkResponses in masterChunkReplies channel and job was already ended which resulted into weird and not deterministic situation regarding the job result.

## Spring components necessary to define at Slave Nodes ##

Spring Integration configuration at slaves logically must begin **with getting the ChunkRequests from master node**, see JMS message driven adapter "**slaveRequests**" in the following example. Also we need to define JMS outbound channel adapter for publishing the replies back to master, see "slaveOutgoingReplies" in the example below.

```
<!-- Slave request messages begin here -->
    <jms:message-driven-channel-adapter id="slaveRequests"
                                        connection-factory="remoteChunkingConnectionFactory"
                                        destination="remoteChunkingRequestsQueue"
                                        channel="chunkRequests"
                                        concurrent-consumers="10"
                                        max-concurrent-consumers="50"
                                        receive-timeout="5000"
                                        idle-task-execution-limit="10"
                                        idle-consumer-limit="5"

            />

    <int:channel id="chunkRequests" />
    <int:channel id="chunkReplies">
        <int:interceptors>
            <int:wire-tap channel="loggingChannel"/>
        </int:interceptors>
    </int:channel>

    <jms:outbound-channel-adapter id="slaveOutgoingReplies"
                                  connection-factory="remoteChunkingConnectionFactory"
                                  destination="remoteChunkingRepliesQueue"
                                  channel="chunkReplies"
            />

    <int:logging-channel-adapter id="loggingChannel" level="INFO" log-full-message="true"/>
```

### Recommendations ###

I highly** recommend not to follow the examples at github** and use JMS message driven adapter with multithreaded receivers instead of JMS pooling, especially if you've got really fast master readers. It really speeds-up the receiving process.

### Configuring batch processing at Slave nodes... ###

Now since we've got defined integration part with master, we can continue with own batch slave configuration. You can do it like this:


```
<!-- Slave part of the job -->
    <int:service-activator id="srvActivator"
                           input-channel="chunkRequests"
                           output-channel="chunkReplies"
                           ref="slaveChunkHandler" method="handleChunk"/>

    <bean id="slaveImportOfferLimitClientHibernateWriter"
          class="net.homecredit.hs.core.bl.service.productoffer.limits.batch.ImportOfferLimitClientHibernateWriter">
        <property name="writerName" value="slave-writer" />
        <property name="isSlaveWriter" value="true" />
    </bean>

    <bean id="slaveChunkHandler"
          class="org.springframework.batch.integration.chunk.ChunkProcessorChunkHandler">
        <property name="chunkProcessor" >
            <bean class="org.springframework.batch.core.step.item.SimpleChunkProcessor">
                <property name="itemWriter" ref="slaveImportOfferLimitClientHibernateWriter"/>
                <property name="itemProcessor">
                    <bean class="org.springframework.batch.item.support.PassThroughItemProcessor"/>
                </property>
            </bean>
        </property>
    </bean>
```
Let's explain previous Spring configuration fragment. Bean with id "**srvActivator**" is a service activator which is activated with receiving ChunkRequest object from channel chunkRequests, this channel is feeded by JMS message driven adapter with id slaveRequests, remember? This service activator passes this request to "**slaveChunkHandler**" where ChunkRequest is handed over to **ChunkProcessorChunkHandler**. Here you can come up with your processor and writer you want to run at slave nodes, see **itemProcessor** and **itemWriter** properties definition.

### RemoteChunkHandlerFactoryBean (master node) ###

And what about RemoteChunkHandlerFactoryBean? Well, this bean factory is maybe the most important part so far. Because this bean actually turns your local chunk-oriented step into remotely chunked. **And without need of changing your step configuration**! Pretty good, isn't it? And how does it work? Well, **RemoteChunkHandlerFactoryBean removes item-processor and item-writer from your step and puts there PassThroughItemProcessor and ChunkWriter into their place.** This isn't very well documented, but works it in this way. See following code from RemoteChunkHandlerFactoryBean which does that replacing:


```
/**
	 * Replace the chunk processor in the tasklet provided with one that can act as a master in the Remote Chunking
	 * pattern.
	 * 
	 * @param tasklet a ChunkOrientedTasklet
	 * @param chunkWriter an ItemWriter that can send the chunks to remote workers
	 * @param stepContributionSource a StepContributionSource used to gather results from the workers
	 */
	private void replaceChunkProcessor(ChunkOrientedTasklet<?> tasklet, ItemWriter<T> chunkWriter,
			final StepContributionSource stepContributionSource) {
		setField(tasklet, "chunkProcessor", new SimpleChunkProcessor<T, T>(new PassThroughItemProcessor<T>(),
				chunkWriter) {
			@Override
			protected void write(StepContribution contribution, Chunk<T> inputs, Chunk<T> outputs) throws Exception {
				doWrite(outputs.getItems());
				// Do not update the step contribution until the chunks are
				// actually processed
				updateStepContribution(contribution, stepContributionSource);
			}
		});
	}
```


### ChunkRequests are load-balanced between master and slaves in Round-Robin manner, yeah! ###

This is certainly something you'd expect from your spring job scalling, right? Let's see how does it work, take a closer look at following two configuration fragments:

```
  <int:service-activator
            id="masterServiceActivator"
            input-channel="masterChunkRequests"
            output-channel="masterChunkReplies"
            ref="masterChunkHandler"
            />

    <bean id="masterChunkHandler" class="org.springframework.batch.integration.chunk.RemoteChunkHandlerFactoryBean">
        <property name="chunkWriter" ref="chunkWriter" /> 
        <property name="step" ref="stepWriteClientDataTransferItems" />
    </bean>
```

```
    <!-- Outbound channel adapter for sending requests (chunks) -->
    <jms:outbound-channel-adapter id="masterJMSRequests"
                                  channel="masterChunkRequests"
                                  connection-factory="remoteChunkingConnectionFactory"
                                  destination="remoteChunkingRequestsQueue"
            />
```

Well, "masterJMSRequests" is a Direct Channel, remember? With previous configurations, this channel has **two subscribers**, one which sends ChunkRequests to slaves(**outbound-channel-adapter**) and one which sends ChunkRequests for processing at master (**service-activator**) Now comes part which isn't documented very well, again. **These two subscribers receives ChunkRequests in round-robin manner by default, yes, MASTER AND SLAVES receives ChunkRequests on circular basis!** In case of service-activator, processing takes place at master node, chunk is processed as your local version of your step defines...Do you get the idea now? Processing at slaves you define by entering the flow after consuming ChunkRequest from queue at slaves via ChunkProcessorChunkHandler, see **srvActivator** bean. You still don't get it? See the following example:

We've got for example Spring batch Step1, with **local definition**

```
reader = A
processor = B
writer = C

```

Now with definition of RemoteChunkHandlerFactoryBean, Step1 changes into:

```
reader = A
processor = PassThroughItemProcessor
writer = chunk-writer, publishing ChunkRequests to slaves

```
Mentioned chunk-writer delegates into "**masterChunkRequests**" channel. If subscriber of that channel "masterJMSRequests" is invoked, request is send to slaves via JMS, if substriber "masterServiceActivator" is invoked, request is send to master and it's processed via your local version of your step. And that's it!

Guys, you won't find better description on the internet, because people just posts remote chunking examples, but no one was actually able to explain it in detail. I hope you found in my description everything you needed.

Best Regards

Tomas