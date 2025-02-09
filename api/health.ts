import {
    initialize,
    requestPermission,
    readRecords,
    insertRecords,
} from 'react-native-health-connect';

const init = async (ignorePermissions?: boolean) => {
    // initialize the client
    console.log("waiting for initialization");
    const isInitialized = await initialize();

    if (ignorePermissions) {
        return;
    }

    // request permissions
    console.log("waiting for permissions");
    const grantedPermissions = await requestPermission([
        { accessType: 'read', recordType: 'Steps' },
        { accessType: 'write', recordType: 'Steps' }
    ]);

    // check if granted
    console.log("permissions", grantedPermissions.length);
    grantedPermissions.forEach(p => {
        console.log("permission", p.accessType, p.recordType);
    });
}

export const readSteps = async (ignorePermissions?: boolean) => {
    await init(ignorePermissions);

    const now = new Date();
    const timeDiff = now.getTime() % (1000 * 60 * 60 * 24);
    const today = new Date(new Date().getTime() - timeDiff);

    console.log("waiting for results");
    const {records: result} = await readRecords('Steps', {
        timeRangeFilter: {
            operator: 'between',
            startTime: today.toISOString(),
            endTime: now.toISOString(),
        }
    });

    return result;
};

export interface StepData {
    from: Date,
    to: Date,
    count: number
}

export const writeSteps = async (steps: StepData[], ignorePermissions?: boolean) => {
    await init(ignorePermissions);

    console.log("adding", steps.length, "records");
    await insertRecords(steps.map(record => ({
        startTime: record.from.toISOString(),
        endTime: record.from.toISOString(),
        recordType: "Steps",
        count: record.count
    })));
}