from threading import Event

from scheduler import start_scheduler


if __name__ == "__main__":
    print("Starting scheduler service...", flush=True)

    start_scheduler()

    print("Scheduler service is running.", flush=True)

    # Keep this process alive
    Event().wait()