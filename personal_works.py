TOTAL_WORK_DAYS = 0
TOTAL_WORK_TIME = 0
EXPECTED_WORK_DAYS = 0

def cal_work_days(check=0):
    global TOTAL_WORK_DAYS, EXPECTED_WORK_DAYS
    if EXPECTED_WORK_DAYS == 0:
        total_work_days = int(input("Monthly working days: "))
        EXPECTED_WORK_DAYS = total_work_days
    else:
        total_work_days = EXPECTED_WORK_DAYS
    leaves = int(input("Leaves taken: "))
    if TOTAL_WORK_DAYS ==  0: 
        TOTAL_WORK_DAYS = round((total_work_days - leaves)*0.6)
    if check:
        return TOTAL_WORK_DAYS
    else:
        print(f"Work for {round((total_work_days - leaves)*0.6)} days")
    
def office_duration_cal(check=0):
    logged_in_time = 0
    global TOTAL_WORK_TIME
    global TOTAL_WORK_DAYS
    if TOTAL_WORK_DAYS == 0:
        days_present = int(input("Number of days reported: "))
        TOTAL_WORK_DAYS = days_present
    else:
        days_present = TOTAL_WORK_DAYS
    if (TOTAL_WORK_TIME ==0):
        for i in range(1,days_present+1):
            time = input(f"Enter day-{i} timing in hh.mm.ss: ")
            hrs,min,sec = tuple(map(int, time.split('.'))) 
            logged_in_time += (hrs*3600) + (min *60) + sec
            TOTAL_WORK_TIME = logged_in_time
    avg_logged = TOTAL_WORK_TIME / days_present
    hrs,reminder = divmod(avg_logged,3600) 
    min,sec = divmod(reminder,60) 
    if not check:
        print(f"You worked {hrs:2.0f}:{min:2.0f}:{sec:2.0f} on average")
    else:
        return logged_in_time,days_present

def check_future_hrs(avg_hrs_needed):
    to_work = cal_work_days(1)
    worked_time,worked_days = office_duration_cal(1)
    sec_needed = ((avg_hrs_needed*to_work)*3600) - worked_time
    hrs,reminder = divmod(sec_needed,3600)
    min,sec = divmod(reminder,60)
    print(f"You need to work for {hrs}:{min}:{sec}")
    if sec_needed <= 0:
        print("You have already completed your required hours!")
        return
    avg_needed = sec_needed/(to_work - worked_days)
    hrs,reminder = divmod(avg_needed,3600)
    min,sec = divmod(reminder,60)
    print(f"You need to work for {hrs:.0f}:{min:.0f}:{sec:.0f} for {to_work - worked_days} each day")

if __name__ == '__main__':
    start = True
    while start:
        print("Please enter the number of the feature you want to utilize")
        option = input("What is the feature you want to utilize : \n1. Calculate numbers of days to report to office \n2. Calculate Avg time worked in the office \n3. Calculate the time needed to log in the ofc\n4.Exit application\n")
        match(option):
            case ("1"):
                cal_work_days()
                print("-"*10+" back to main menu ".upper()+"-"*10)
            case ("2"):
                office_duration_cal()
                print("-"*10+" back to main menu ".upper()+"-"*10)
            case ("3"):
                avg_hrs_needed = int(input("Enter the average hr you need to maintain\n"))
                check_future_hrs(avg_hrs_needed)
                print("-"*10+" back to main menu ".upper()+"-"*10)
            case ("4"):
                print("-"*10+"Closing the application".upper()+"-"*10)
                start = False
            case _:
                print("Please enter a proper response") 
                print("-"*10+" back to main menu ".upper()+"-"*10)

