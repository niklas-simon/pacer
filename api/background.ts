import BackgroundFetch from 'react-native-background-fetch';
import { load, save } from './storage';
import { Options } from '@/app/index';
import { readSteps, StepData, writeSteps } from './health';

export interface TaskInfo {
    lastExecution: string,
    stepsAdded: number
}

const interval = 15;

function timeToDate(time: string) {
    const timeParts = time.split(":").map(Number);
    const today = Math.trunc(new Date().getTime() / 1000 / 60 / 60 / 24) * 1000 * 60 * 60 * 24;
    return new Date(today + (timeParts[0] * 60 + timeParts[1]) * 60 * 1000);
}

export function configure() {
    console.log("configuring background task");
    return BackgroundFetch.configure(
        {
            minimumFetchInterval: interval, // minimum interval in minutes
            stopOnTerminate: false, // continue running after the app is terminated
            startOnBoot: true, // start when the device boots up
            enableHeadless: true
        },
        async (taskId) => {
            // Perform your background task here
            console.log(`Background task with ID ${taskId} executed`);
    
            const options = await load<Options>("options");
            const info = await load<TaskInfo>("taskInfo");

            const startTime = timeToDate(options.from);
            const lastExecution = new Date(info.lastExecution);
            const addFrom = startTime < lastExecution ? lastExecution : startTime;
            const now = new Date();

            console.log("adding steps starting at " + addFrom.toString());
    
            if (now < startTime) {
                console.log("it's still too early to add steps");
                BackgroundFetch.finish(taskId);
                return;
            }
    
            const endTime = timeToDate(options.to);
            if (now >= endTime) {
                console.log("it's already too late to add steps");
                BackgroundFetch.finish(taskId);
                return;
            }
    
            const currentSteps = (await readSteps(true)).reduce((p, c) => p + c.count, 0);
            const neededSteps = options.target - currentSteps;
    
            if (neededSteps <= 0) {
                console.log("all steps have been added for today");
                BackgroundFetch.finish(taskId);
                return;
            }

            console.log("still needs", neededSteps, "steps");
    
            const timeRemaining = endTime.getTime() - addFrom.getTime();
    
            const stepsPerMinute = neededSteps * 1000 * 60 / timeRemaining;
            const minutesSinceLastExec = (now.getTime() - addFrom.getTime()) / 1000 / 60;
    
            let stepsToAdd = Math.round(minutesSinceLastExec * stepsPerMinute * (Math.random() * 0.2 + 1));
            const stepsAdded = stepsToAdd;
            const partitions = Math.round(minutesSinceLastExec / 5);
            const partitionDuration = Math.trunc(minutesSinceLastExec / partitions);

            console.log("will be adding", stepsAdded, "in", partitions, "partitions with a duration of", partitionDuration, "minutes");
    
            const stepData: StepData[] = [];
            for (let i = 0; i < partitions; i++) {
                const fromMinutes = partitionDuration * i + Math.round(Math.random());
                const toMinutes = partitionDuration * (i + 1) - Math.round(Math.random());
                const stepsInPartition = (i === partitions - 1) ? stepsToAdd : (stepsToAdd / (partitions - i)) * (Math.random() * 0.2 + 0.9);
                stepData.push({
                    from: new Date(addFrom.getTime() + fromMinutes * 60 * 1000),
                    to: new Date(addFrom.getTime() + toMinutes * 60 * 1000),
                    count: stepsInPartition
                });
                stepsToAdd -= stepsInPartition;
            }

            await writeSteps(stepData, true);
    
            await save<TaskInfo>("taskInfo", {
                lastExecution: now.toString(),
                stepsAdded
            });

            console.log("done");
    
            BackgroundFetch.finish(taskId); // signal task completion
        },
        taskId => {
            console.log("timeout");
            BackgroundFetch.finish(taskId);
        }
    );
}

export function start() {
    // Start the background task
    console.log("starting background task");
    return BackgroundFetch.start();
}

export function status() {
    return BackgroundFetch.status();
}

export function stop() {
    console.log("stopping background task");
    return BackgroundFetch.stop();
}