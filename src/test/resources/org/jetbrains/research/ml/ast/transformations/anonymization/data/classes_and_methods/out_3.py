class c1:
    def c1_f1(self):
        c1_f1_v1, c1_f1_v2 = 1, 2


class c2(c1):
    def c1_f1(self):
        c1_f1_v1, c1_f1_v2 = 3, 4


(c1() if input() else c2()).c1_f1()