import Storage from "react-native-storage";
import AsyncStorage from "@react-native-async-storage/async-storage";
import { Options } from "@/app/index";
import { TaskInfo } from "./background";

const storage = new Storage({
    storageBackend: AsyncStorage,
    defaultExpires: null,
    sync: {
        options: () => {
            return {
                target: 10000,
                from: "06:00",
                to: "22:00"
            } satisfies Options
        },
        taskInfo: () => {
            return {
                lastExecution: new Date(0).toString(),
                stepsAdded: 0
            } satisfies TaskInfo
        }
    },
});

export async function save<T>(key: string, data: T) {
    await storage.save({ key, data });
}

export async function load<T>(key: string): Promise<T> {
    return await storage.load({ key });
}
