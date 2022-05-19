# This script takes care of cleaning up the extra build dependencies present in a target.
# Downside of this script is that if there are indirect dependencies, direct ones would also be deleted.
# Deleting direct dependencies is not a good thing, because if the underlying library cleans up its dependencies,
# that can lead to build failures in the top one.
import os
import sys

# Assert that there is at least argument to the script.
assert len(sys.argv) > 1
target = sys.argv[1]

# Create a list of the dependencies.
print_deps_output = os.popen("buildozer 'print deps' " + target).read()
deps = print_deps_output[1:-1].split(" ")

# Try removing them one by one and perform a build.
# If the build succeeds, remove the dependency, else keep it.
for dep in deps:
    print("\nChecking " + dep)
    os.system("buildozer 'remove deps " + dep + "' " + target + " > /dev/null 2>&1")
    status = os.system("bazel build " + target + " > /dev/null 2>&1")
    if status != 0:
        os.system("buildozer 'add deps " + dep + "' " + target + " > /dev/null 2>&1")
    else:
        print("Removed: " + dep)

print(target + " optimization is complete!")