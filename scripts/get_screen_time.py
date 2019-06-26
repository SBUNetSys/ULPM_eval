import ast
import numpy as np
import os
import xml.etree.ElementTree as ET


KEYBOARD_TYPES = ["screen_off_arbitrary"]
KEYBOARD_TYPES_OUTPUT = ["arbitrary"]
BLOCKS = ["1", "2", "3", "4"]


def read_data():
    root_dir = os.getcwd()
    data_dir = os.path.join(root_dir, "./data")
    res_dir = os.path.join(root_dir, "./results")

    file_names = []
    outputs = []
    for item in KEYBOARD_TYPES_OUTPUT:
        file_names.append("screen_data_" + item + ".txt")
    for file_name in file_names:
        output = open(res_dir + os.sep + file_name, "w")
        output.write("#user_id, block_num, trial_num, stimulus, screen_off_count, "
                     "average_screen_on_duration, total_screen_on_duration, word_count, total_time\n")
        outputs.append(output)

    for subdir, dirs, files in os.walk(data_dir):
        for file in sorted(files):
            filepath = subdir + os.sep + file
            # Filter files
            if ".xml" not in filepath or "Screen" not in filepath:
                continue

            tree = ET.parse(filepath)
            root = tree.getroot()
            task_type = root.find('taskType').text
            # processing T40 task only
            if task_type != "1":
                continue

            user_id = root.find('user').text
            keyboard_type = root.find('screenOption').text
            if keyboard_type != "screen_off_arbitrary":
                continue
            keyboard_type_index = KEYBOARD_TYPES.index(keyboard_type)

            for trial in root.findall('trial'):
                # block_num = trial.find('blockNum').text
                trial_num = trial.find('trialNum').text
                block_num_int = (int(trial_num) - 4) / 8 + 1
                block_num = str(block_num_int)
                # discard warm up
                if block_num == "0":
                    continue
                stimulus = trial.find('stimulus').text
                if stimulus.endswith("."):
                    stimulus = stimulus[:-1]

                count = 0.0
                screenOnDuration = 0.0
                isScreenOn = trial.findall('isScreenOn')
                screenTime = trial.findall('time')
                if len(isScreenOn) < 2:
                    continue
                firstTime = screenTime[0].text
                lastTime = screenTime[len(screenTime) - 1].text

                timer = 0.0
                for i in xrange(0, len(isScreenOn)):
                    if isScreenOn[i].text == "1" and screenTime[i].text != lastTime:
                        # screen turned on
                        timer = long(screenTime[i].text)
                    if isScreenOn[i].text == "0" and screenTime[i].text != firstTime:
                        # screen turned off
                        if timer == 0:
                            print "???"
                        screenOnDuration += long(screenTime[i].text) - timer
                        count += 1
                        timer = 0.0

                if count != 0:
                    outputs[keyboard_type_index].write(user_id + ", " +
                                                       block_num + ", " +
                                                       trial_num + ", " +
                                                       stimulus + ", " +
                                                       str(count) + ", " +
                                                       str(screenOnDuration / float(count)) + ", " +
                                                       str(screenOnDuration) + ", " +
                                                       str(len(stimulus) / 5.0) + ", " +
                                                       str(long(lastTime) - long(firstTime))+ "\n")
                else:
                    outputs[keyboard_type_index].write(user_id + ", " +
                                                       block_num + ", " +
                                                       trial_num + ", " +
                                                       stimulus + ", " +
                                                       "0, 0, 0, " +
                                                       str(len(stimulus) / 5.0) + ", " +
                                                       str(long(lastTime) - long(firstTime))+ "\n")


    # Close output file
    for output in outputs:
        output.close()


def get_wpm_wer():
    root_dir = os.getcwd()
    res_dir = os.path.join(root_dir, "./results")

    output_sum = open(res_dir + os.sep + "screen_time_user_mean.txt", "w")
    output_sum.write("user\tscreen_on_count\taverage_screen_on_duration\ttotal_screen_on_duration\ttotal_time\tfrequency\n")
    output_block = open(res_dir + os.sep + "screen_time_user_block_mean.txt", "w")
    output_block.write("user\tblock\tscreen_on_count\taverage_screen_on_duration\ttotal_screen_on_duration\ttotal_time\tfrequency\n")

    for item in KEYBOARD_TYPES_OUTPUT:
        kb_index = KEYBOARD_TYPES_OUTPUT.index(item)
        users = []
        # 2d lists: user x block
        total_count = []
        total_duration = []
        total_time = []

        input_filename = "screen_data_" + item + ".txt"
        input_f = open(res_dir + os.sep + input_filename, "r")

        for line in input_f:
            if line.startswith("#") or line == "\n":
                continue

            s = line.strip().split(', ')
            user_id = s[0].strip()
            if user_id not in users:
                users.append(user_id)
                user_index = users.index(user_id)
                total_count.append([])
                total_duration.append([])
                total_time.append([])
                for i in xrange(0, len(BLOCKS)):
                    total_count[user_index].append(0)
                    total_duration[user_index].append(0)
                    total_time[user_index].append(0)

            user_index = users.index(user_id)
            block_num = s[1].strip()
            block_index = BLOCKS.index(block_num)
            count = float(s[4].strip())
            duration = float(s[6].strip())
            submit_time = float(s[8].strip())

            total_count[user_index][block_index] += count
            total_duration[user_index][block_index] += duration
            total_time[user_index][block_index] += submit_time
        # end for line

        for u_index in xrange(0, len(users)):
            u_c = []
            u_ad = []
            u_td = []
            u_tt = []
            for b_index in xrange(0, len(BLOCKS)):
                # per user per block
                count = total_count[u_index][b_index]
                total = total_duration[u_index][b_index]
                u_c.append(count)
                u_td.append(total)
                ave = 0
                if count != 0:
                    ave = total / count
                u_ad.append(ave)
                u_tt.append(total_time[u_index][b_index])
                if count != 0:
                    output_block.write(users[u_index] + "\t" +
                                       BLOCKS[b_index] + "\t" +
                                       "{0:.2f}".format(count) + "\t" +
                                       "{0:.2f}".format(ave) + "\t" +
                                       "{0:.2f}".format(total) + "\t" +
                                       "{0:.2f}".format(total_time[u_index][b_index]) + "\t" +
                                       "{0:.2f}".format(total_time[u_index][b_index] / (count + 1)) + "\n")
                else:
                    output_block.write(users[u_index] + "\t" +
                                       BLOCKS[b_index] + "\t" +
                                       "{0:.2f}".format(count) + "\t" +
                                       "{0:.2f}".format(ave) + "\t" +
                                       "{0:.2f}".format(total) + "\t" +
                                       "{0:.2f}".format(total_time[u_index][b_index]) + "\t" +
                                       "{0:.2f}".format(total_time[u_index][b_index]) + "\n")
            if np.sum(u_c) != 0:
                output_sum.write(users[u_index] + "\t" +
                                 "{0:.0f}".format(np.sum(u_c)) + "\t" +
                                 "{0:.10f}".format((np.sum(u_td) / np.sum(u_c))) + "\t" +
                                 "{0:.0f}".format(np.sum(u_td)) + "\t" +
                                 "{0:.0f}".format(np.sum(u_tt)) + "\t" +
                                 "{0:.0f}".format(np.sum(u_tt) / (np.sum(u_c) + 1)) + "\n")
            else:
                output_sum.write(users[u_index] + "\t" +
                                 "{0:.0f}".format(np.sum(u_c)) + "\t" +
                                 "{0:.0f}".format(0) + "\t" +
                                 "{0:.0f}".format(np.sum(u_td)) + "\t" +
                                 "{0:.0f}".format(np.sum(u_tt)) + "\t" +
                                 "{0:.0f}".format(np.sum(u_tt)) + "\n")

        input_f.close()
    output_block.close()
    output_sum.close()


def main():
    read_data()
    get_wpm_wer()


if __name__ == "__main__":
    main()
