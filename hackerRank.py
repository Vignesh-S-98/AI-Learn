#### TASK 1 ####
'''

'''
def task1():
    if __name__ == '__main__':
        x = int(input())
        y = int(input())
        z = int(input())
        n = int(input())
        permutations = []
        for i in range(x+1):
            for j in range(y+1):
                for k in range(z+1):
                    if (i+j+k) != n:
                        permutations.append(list([i,j,k]))
        print(permutations)
 
#### TASK 2 ####
'''
Given the participants' score sheet for your University Sports Day, you are required to find the runner-up score. You are given  scores. Store them in a list and find the score of the runner-up.

Input Format

The first line contains . The second line contains an array   of  integers each separated by a space.

Constraints

Output Format

Print the runner-up score.
'''
def task2():
    if __name__ == '__main__':
        n = int(input())
        arr = map(int, input().split())
        # n = 5
        # arr = [2, 3, 6, 6, 5]
        A = list(arr)
        if n<2 or n >10:
            return
        if len(A) < -100 or len(A)>100 or len(A)!=n:
            return
        scores = sorted(set(A),reverse=True)
        print(scores)
        print(scores[1])


#### TASK 3 ####

'''
Given the names and grades for each student in a class of  students, store them in a nested list and print the name(s) of any student(s) having the second lowest grade.

Note: If there are multiple students with the second lowest grade, order their names alphabetically and print each name on a new line.

Example

The ordered list of scores is , so the second lowest score is . There are two students with that score: . Ordered alphabetically, the names are printed as:

alpha
beta
Input Format

The first line contains an integer, , the number of students.
The  subsequent lines describe each student over  lines.
- The first line contains a student's name.
- The second line contains their grade.

Constraints

There will always be one or more students having the second lowest grade.
Output Format

Print the name(s) of any student(s) having the second lowest grade in. If there are multiple students, order their names alphabetically and print each one on a new line.
'''
def task3():
    if __name__ == '__main__':
        names = []
        scores = [] 
        result= []
        second_lowest=[]
        N = int(input())
        if N>=2 or N<= 5 : 
            for _ in range(N):
                name = input()
                score = float(input())
                names.append(name)
                scores.append(score)
            sorted_score = sorted(scores,reverse=False)
            for i in range(len(sorted_score)):
                if sorted_score[i] != sorted_score[0]:
                    result.append(sorted_score[i])
            second_score = result[0]
            indexes = [index for index,value in enumerate(scores)  if value == second_score]
            for index in indexes:
                second_lowest.append(names[index])
            for name in sorted(second_lowest):
                print(name)


#### TASK 4 ####
'''
The provided code stub will read in a dictionary containing key/value pairs of name:[marks] for a list of students. Print the average of the marks array for the student name provided, showing 2 places after the decimal.

Example

marks key:value pairs are
'alpha': [20, 30, 40]
'beta': [30, 50, 70]
query_name = 'beta'


The query_name is 'beta'. beta's average score is  (30 + 50 + 70)/3 = 50.0..

Input Format

The first line contains the integer , the number of students' records. The next  lines contain the names and marks obtained by a student, each value separated by a space. The final line contains query_name, the name of a student to query.

Constraints
. 2≤n≤ 10
. 0 ≤ marks[i] ≤ 100
. length of marks arrays =3

Output Format

Print one line: The average of the marks obtained by the particular student correct to 2 decimal places.

Example input 
Harsh 25 26.5 28
Marsh 54 43.5 57 
'''

def task4():
    if __name__ == '__main__':
        n = int(input())
        student_marks = {}
        if (n<2) or (n>10):
            exit()
        for _ in range(n):
            name, *line = input().split()
            scores = list(map(float, line))
            if len(scores) != 3:
                exit() 
            for i in range(len(scores)):
                if scores[i] > 100 or scores[i]<0:
                    exit()
            student_marks[name] = scores
        query_name = input()
        avg_mark = sum(student_marks[query_name])/len(student_marks[query_name])
        print(f"{avg_mark:.2f}")


#### TASK 5 ####
'''
Consider a list (list = []). You can perform the following commands:

insert i e: Insert integer  at position .
print: Print the list.
remove e: Delete the first occurrence of integer .
append e: Insert integer  at the end of the list.
sort: Sort the list.
pop: Pop the last element from the list.
reverse: Reverse the list.
Initialize your list and read in the value of  followed by  lines of commands where each command will be of the  types listed above. Iterate through each command in order and perform the corresponding operation on your list.

Example
N= 4

append 1
append 2

insert 1 3
print
. append 1: Append 1 to the list, arr = [1].
. append 2: Append 2 to the list, arr = [1,2].
. insert 1 3: Insert 3 at index 1, arr = [1,3,2].
: Print the array.
Output:
[1, 3, 2]
Input Format

The first line contains an integer, , denoting the number of commands.
Each line  of the  subsequent lines contains one of the commands described above.

Constraints

The elements added to the list must be integers.
Output Format

For each command of type print, print the list on a new line.

Sample Input 0

12
insert 0 5
insert 1 10
insert 0 6
print
remove 6
append 9
append 1
sort
print
pop
reverse
print
Sample Output 0

[6, 5, 10]
[1, 5, 9, 10]
[9, 5, 1]
'''
def task5():
    if __name__ == '__main__':
        N = int(input())
        arr = []
        for _ in range(N):
            query, *values = input().split()
            index = None
            value = None
            if  len(values)==2:
                index = int(values[0])
                value = int(values[1])
            elif len(values)==1:
                value = int(values[0])
            elif len(values)>2:
                exit()
            match query.lower():
                case "insert":
                    arr.insert(index,value)
                case "print" :
                    print(arr)
                case "remove":
                    arr.remove(value)
                case "append":
                    arr.append(value)
                case "sort":
                    arr.sort()
                case "pop":
                    arr.pop()
                case "reverse":
                    arr.reverse()


task5()