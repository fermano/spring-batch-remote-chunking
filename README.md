# Scaling jobs with Spring Batch Remote Chunking #

Recently in IT company where I work, I was asked to **add Spring Batch Remote Chunking support** into our system. So I just want to share my experience with that, make a demo and give some recommendations. First just briefly, Spring Batch Remote Chunking is way of making **running Spring Batch jobs in parallel at N remote machines concurrently**. Sounds good? Allright, let's get to business. 
What do we need for it? First of all middleware for publishing chunks to remote machines. Almost everyone is using **JMS** for that, using this approach **you need to define two JMS queues**, one for publishing chunk requests to slaves and one for getting replies from slaves about how they processed that particular chunk. 

## Spring components necessary to define at Master Node  ##
