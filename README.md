# Scaling jobs with Spring Batch Remote Chunking #

Recently in IT company where I work, I was asked to **add Spring Batch Remote Chunking support** into our system. So I just want to share my experience with that, make a demo and give some recommendations. First just briefly, Spring Batch Remote Chunking is way of making **running Spring Batch jobs in parallel at N remote machines concurrently**. Sounds good? Allright, let's get to business. 
What do we need for it? First of all middleware for publishing chunks to remote machines. Almost everyone is using **JMS** for that. The component's necessary at master and slaves are the following:
