import ast
import numpy as np
import os
import xml.etree.ElementTree as ET


KEYBOARD_TYPES = ["screen_on", "screen_off", "screen_off_wordwise", "screen_off_arbitrary"]
KEYBOARD_TYPES_OUTPUT = ["on", "off", "wordwise", "arbitrary"]
BLOCKS = ["1", "2", "3", "4"]


def minimum_word_distance(target, source):
    """ Computes the min word distance from target to source. """
    n = len(target)
    m = len(source)
    distance = [[0 for i in range(m + 1)] for j in range(n + 1)]

    for i in range(1, n + 1):
        distance[i][0] = distance[i-1][0] + 1 # insert cost
    for j in range(1, m + 1):
        distance[0][j] = distance[0][j-1] + 1 # delete cost
    for i in range(1, n + 1):
        for j in range(1, m + 1):
            distance[i][j] = min(distance[i-1][j]+1,
                                 distance[i][j-1]+1,
                                 distance[i-1][j-1] + substitution_cost(source[j-1],target[i-1]))
    return distance[n][m]


def substitution_cost(source, target):
    if source == target:
        return 0
    else:
        return 1


def get_time_filter():
    root_dir = os.getcwd()
    res_dir = os.path.join(root_dir, "./results")

    time_filter = [] # holds one dictionary per keyboard type
    for item in KEYBOARD_TYPES_OUTPUT:
        time_filter.append({})
        kb_index = KEYBOARD_TYPES_OUTPUT.index(item)

        phrases = []
        phrase_time = []

        input_filename = "phrase_data_" + item + ".txt"
        with open(res_dir + os.sep + input_filename, "r") as input_f:
            for line in input_f:
                if line.startswith("#") or line == "\n":
                    continue
                s = line.strip().split(',')
                phrase = s[4].strip()
                if phrase not in phrases:
                    phrases.append(phrase)
                    phrase_time.append([])
                p_index = phrases.index(phrase)
                duration = int(s[8].strip())
                phrase_time[p_index].append(duration)
            # end for line

        for p in xrange(0, len(phrases)):
            phrase = phrases[p]
            m = np.mean(phrase_time[p])
            std = np.std(phrase_time[p])
            time_filter[kb_index][phrase] = [m, std]

    return time_filter


def read_data():
    root_dir = os.getcwd()
    data_dir = os.path.join(root_dir, "./data")
    res_dir = os.path.join(root_dir, "./results")

    file_names = []
    outputs = []
    for item in KEYBOARD_TYPES_OUTPUT:
        file_names.append("phrase_data_" + item + ".txt")
    for file_name in file_names:
        output = open(res_dir + os.sep + file_name, "w")
        output.write("#user_id, input_finger, block_num, trial_num, stimulus, "
                     "committed_text, start_time, end_time, duration, MWD, delete_count\n")
        outputs.append(output)

    for subdir, dirs, files in os.walk(data_dir):
        for file in sorted(files):
            filepath = subdir + os.sep + file
            # Filter files
            if ".xml" not in filepath or "Touch_Point" not in filepath:
                continue

            tree = ET.parse(filepath)
            root = tree.getroot()

            task_type = root.find('taskType').text
            # processing T40 task only
            if task_type != "1":
                continue

            user_id = root.find('user').text
            input_finger = root.find('inputFinger').text
            keyboard_type = root.find('screenOption').text
            keyboard_type_index = KEYBOARD_TYPES.index(keyboard_type)

            for trial in root.findall('trial'):
                #block_num = trial.find('blockNum').text
                trial_num = trial.find('trialNum').text
                block_num_int = (int(trial_num) -4)/8 + 1
                block_num = str(block_num_int)
                is_canceled = trial.find('isCanceled').text
                # discard canceled trials and warm up
                if is_canceled == "1" or block_num == "0":
                    continue
                stimulus = trial.find('stimulus').text
                if stimulus.endswith("."):
                    stimulus = stimulus[:-1]
                committed_text = trial.find('editTextContent').text.strip().replace('\n', ' ')

                # calculate trial duration
                start_time = 0
                end_time = 0
                for ime_data in trial.findall("imeData"):
                    touch_point = ime_data.findall('touchPoints')
                    # find start time
                    for i in xrange(0, len(touch_point)):
                        if touch_point[i].find('pointCount').text != "0":
                            start_time = long(touch_point[i].find('startTime').text)
                            break
                    # find end time
                    for i in xrange(len(touch_point) - 1, 0, -1):
                        if touch_point[i].find('pointCount').text != "0":
                            end_time = long(touch_point[i].find('startTime').text)
                            break

                # record the number of delete key presses
                delete_count = 0
                for ime_data in trial.findall("imeData"):
                    for function_key in ime_data.findall('functionKey'):
                        if function_key is not None:
                            if function_key.text == "delete":
                                delete_count += 1

                # calculate minimum word edit distance between stimulus and committed_text
                word_distance = minimum_word_distance(committed_text.lower().split(),
                                                      stimulus.lower().split())

                outputs[keyboard_type_index].write(user_id + ", " +
                                                   input_finger + ", " +
                                                   block_num + ", " +
                                                   trial_num + ", " +
                                                   stimulus + ", " +
                                                   committed_text.encode('utf-8') + ", " +
                                                   str(start_time) + ", " +
                                                   str(end_time) + ", " +
                                                   str(end_time - start_time) + ", " +
                                                   str(word_distance) + ", " +
                                                   str(delete_count) + "\n")

    # Close output file
    for output in outputs:
        output.close()


def get_wpm_wer(time_filter):
    root_dir = os.getcwd()
    res_dir = os.path.join(root_dir, "./results")

    output_sum = open(res_dir + os.sep + "anova_user_mean.txt", "w")
    output_sum.write("user\twpm\twer\tdelete_per_word\tkeyboard_type\n")
    output_block = open(res_dir + os.sep + "anova_user_block_mean.txt", "w")
    output_block.write("user\tblock\twpm\twer\tdelete_per_word\tkeyboard_type\n")

    for item in KEYBOARD_TYPES_OUTPUT:
        print item
        A = 0
        kb_index = KEYBOARD_TYPES_OUTPUT.index(item)
        users = []
        # 2d lists: user x block
        total_length = []
        total_time = []
        total_error_count = []
        total_word_count = []
        total_delete_count = []

        input_filename = "phrase_data_" + item + ".txt"
        input_f = open(res_dir + os.sep + input_filename, "r")
        output_file = "wpm_wer_" + item + ".txt"
        output = open(res_dir + os.sep + output_file, "w")
        output.write("user\tblock\twpm\twer\tdelete_per_word\n")

        for line in input_f:
            if line.startswith("#") or line == "\n":
                continue

            s = line.strip().split(',')
            user_id = s[0].strip()
            if user_id not in users:
                users.append(user_id)
                user_index = users.index(user_id)
                total_length.append([])
                total_time.append([])
                total_error_count.append([])
                total_word_count.append([])
                total_delete_count.append([])
                for i in xrange(0, len(BLOCKS)):
                    total_length[user_index].append(0)
                    total_time[user_index].append(0)
                    total_error_count[user_index].append(0)
                    total_word_count[user_index].append(0)
                    total_delete_count[user_index].append(0)

            user_index = users.index(user_id)
            block_num = s[2].strip()
            block_index = BLOCKS.index(block_num)
            phrase = s[4].strip()
            commit_text = s[5].strip()
            duration = s[8].strip()
            edit_distance = s[9].strip()
            delete_count = int(s[10].strip())

            # save trial data
            m, std = time_filter[kb_index].get(phrase)
            threshold_up = m + std * 2.0
            threshold_lo = m - std * 2.0
            # 95% filter
            if threshold_lo <= long(duration) <= threshold_up:
                # use normalized wpm
                total_length[user_index][block_index] += len(phrase)
                total_time[user_index][block_index] += long(duration)
                total_error_count[user_index][block_index] += float(edit_distance)
                total_word_count[user_index][block_index] += len(phrase.split(" "))
                total_delete_count[user_index][block_index] += delete_count
            else:
                print "slow " + str(A)
                A += 1
        # end for line

        for u_index in xrange(0, len(users)):
            u_wpm = []
            u_wer = []
            u_dpw = []
            u_uncorrected_er = []
            for b_index in xrange(0, len(BLOCKS)):
                # per user per block wpm and wer
                wpm = 0
                err = 0
                dpw = 0
                uncorrected_er = 0
                if total_time[u_index][b_index] != 0:
                    wpm = (total_length[u_index][b_index] / 5.0) / \
                          (total_time[u_index][b_index] / 60000.0)
                    u_wpm.append(wpm)
                if total_word_count[u_index][b_index] != 0:
                    err = total_error_count[u_index][b_index] / float(total_word_count[u_index][b_index])
                    u_wer.append(err)
                    dpw = total_delete_count[u_index][b_index] / float(total_word_count[u_index][b_index])
                    u_dpw.append(dpw)
                output.write(users[u_index] + "\t" +
                             BLOCKS[b_index] + "\t" +
                             "{0:.10f}".format(wpm) + "\t" +
                             "{0:.10f}%".format(err * 100) + "\t" +
                             "{0:.10f}".format(dpw) + "\n")
                output_block.write(users[u_index] + "\t" +
                                   BLOCKS[b_index] + "\t" +
                                   "{0:.2f}".format(wpm) + "\t" +
                                   "{0:.2f}".format(err * 100) + "\t" +
                                   "{0:.2f}".format(dpw) + "\t" +
                                   item + "\n")
            # per user all block wpm and wer
            # across_block_wpm = (sum(total_length[u_index]) / 5.0) / \
            #                    (sum(total_time[u_index]) / 60000.0)
            # across_block_err = sum(total_error_count[u_index]) / sum(total_word_count[u_index])
            output_sum.write(users[u_index] + "\t" +
                             "{0:.10f}".format(np.mean(u_wpm)) + "\t" +
                             "{0:.10f}".format(np.mean(u_wer) * 100) + "\t" +
                             "{0:.10f}".format(np.mean(u_dpw)) + "\t" +
                             item + "\n")
        # per keyboard type wpm and wer
        # overall_wpm = (sum(sum(total_length, [])) / 5.0) / \
        #               (sum(sum(total_time, [])) / 60000.0)
        # overall_err = sum(sum(total_error_count, [])) / sum(sum(total_word_count, []))
        # output.write("-\t-\t" +
        #              "{0:.2f}".format(overall_wpm) + "\t" +
        #              "{0:.2f}%".format(overall_err * 100) + "\n")

        input_f.close()
        output.close()
    output_block.close()
    output_sum.close()


def main():
    read_data()
    time_filter = get_time_filter()
    get_wpm_wer(time_filter)


if __name__ == "__main__":
    main()
