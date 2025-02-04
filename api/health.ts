import {
    initialize,
    requestPermission,
    readRecords,
    insertRecords,
} from 'react-native-health-connect';

const init = async () => {
    // initialize the client
    console.log("waiting for initialization");
    const isInitialized = await initialize();

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

export const readSteps = async () => {
    await init();

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

    console.log("result: ", result);
    return result;
};

export const writeSteps = async (from: Date, to: Date, count: number) => {
    console.log("adding", from.toString(), to.toString(), count);
    const result = await insertRecords([
        {
            recordType: "Steps",
            startTime: from.toISOString(),
            endTime: to.toISOString(),
            count: count
        }
    ]);
    console.log("result", result);
}