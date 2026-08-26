# This files runs independently of main file on AWS EC2, the main goal of this file is to 
# run schedular continuously and check for schedules.

from threading import Event

from scheduler import start_scheduler


if __name__ == "__main__":
    print("Starting scheduler service...", flush=True)

    start_scheduler()

    print("Scheduler service is running.", flush=True)

    # Keep this process alive
    Event().wait()