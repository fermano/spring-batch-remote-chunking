# Scaling jobs with Spring Batch Remote Chunking #

Recently in IT company where I work, I was asked to **add Spring Batch Remote Chunking support** into our system. So I just want to share my experience with that, make a demo and give some recommendations. First just briefly, Spring Batch Remote Chunking is a way of making **Spring Batch jobs running in parallel at N remote machines concurrently**. Sounds good? Allright, let's get to business. What do we need for it? First of all middleware for publishing chunks to remote machines. Almost everyone is using **JMS** for that, using this approach **you need to define two JMS queues**, one for **publishing chunk requests** to slaves and one for getting replies from slaves about **how they processed that particular chunk**. 

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

This bean is in duty of publishing ChunkRequests to slaves(**messagingOperations** parameter) and getting the replies from slaves (**replyChannel** parameter). Basic idea of this class is that there is a class called **LocalState** which has two atomic integers called **expected** and **actual**. Expected is incremented **per sended ChunkRequest** and actual is incremented **per received ChunkResponse**. So substract of **Expected - Actual is how many receivers are currently processing the chunks**. Every now and then we need to wait for them. This is the moment when parameter **throttleLimit** comes in, it says *what is the maximal count of chunk processing receivers we are going to be waiting on before next ChunkRequests will be published again*. It is to avoid the overwhelming the receivers. At the end of a job, when all chunks are published, we need to wait for all results from slaves, how many times we will ask the replyChannel for replies is saved in "**maxWaitTimeouts**" parameter.