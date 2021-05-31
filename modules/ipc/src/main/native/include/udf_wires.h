/*
 * Copyright 2019-2021 tsurugi project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
#pragma once

#include "wire.h"

namespace tsubakuro::common::wire {

class session_wire_container;

class response
{
    static constexpr std::size_t max_responses_size = 256;

public:
    response(session_wire_container *container, std::size_t idx) : container_(container), idx_(idx) {}

    /**
     * @brief Copy and move constructers are deleted.
     */
    response(response const&) = delete;
    response(response&&) = delete;
    response& operator = (response const&) = delete;
    response& operator = (response&&) = delete;
    
    signed char* read();

    void set_length(std::size_t length) { length_ = length; }
    std::size_t get_length() { return length_; }
    signed char* get_buffer() { return buffer_; }

    void set_inuse() { inuse_ = true; }
    bool is_inuse() { return inuse_; }
    void dispose() {
        inuse_ = false;
        length_ = 0;
    }

private:
    bool inuse_{};
    signed char message_{};
    session_wire_container *container_;
    std::size_t idx_;
    signed char buffer_[ max_responses_size];
    std::size_t length_{};
};

class session_wire_container
{
    static constexpr const char* wire_name = "request_response";
    static constexpr std::size_t max_responses_size = 16;

public:
    session_wire_container(std::string_view name, bool owner = false) : owner_(owner), name_(name) {
        try {
            managed_shared_memory_ = std::make_unique<boost::interprocess::managed_shared_memory>(boost::interprocess::open_only, name_.c_str());
            session_wire_ = managed_shared_memory_->find<session_wire>(wire_name).first;
            if (session_wire_ == nullptr) {
                std::abort();  // FIXME
            }
            responses.resize(max_responses_size);
            responses.at(0) = std::make_unique<response>(this, 0);  // prepare one entry that always gets used
        }
        catch(const boost::interprocess::interprocess_exception& ex) {
            std::abort();  // FIXME
        }
    }

    /**
     * @brief Copy and move constructers are deleted.
     */
    session_wire_container(session_wire_container const&) = delete;
    session_wire_container(session_wire_container&&) = delete;
    session_wire_container& operator = (session_wire_container const&) = delete;
    session_wire_container& operator = (session_wire_container&&) = delete;

    ~session_wire_container() {
        if (owner_) {
            boost::interprocess::shared_memory_object::remove(name_.c_str());
        }
    }

    response *write(signed char* msg, std::size_t length) {
        response *r;
        std::size_t idx;
        for (idx = 0 ; idx < responses.size() ; idx++) {
            if((r = responses.at(idx).get()) == nullptr) { goto case_need_create; }
            if(!r->is_inuse()) { goto case_exist; }
        }
        return nullptr;

      case_need_create:
        responses.at(idx) = std::make_unique<response>(this, idx);
        r = responses.at(idx).get();
      case_exist:
        r->set_inuse();
        session_wire_->get_request_wire().write(msg, message_header(idx, length));
        return r;
    }

    void read_all() {
        simple_wire& wire = session_wire_->get_response_wire();
        while (true) {
            message_header h = wire.peep();
            if (h.get_length() == 0) { break; }
            response& r = *responses.at(h.get_idx());
            r.set_length(h.get_length());
            wire.read(r.get_buffer(), h.get_length());
        }
    }

private:
    bool owner_;
    std::string name_;
    std::unique_ptr<boost::interprocess::managed_shared_memory> managed_shared_memory_{};
    session_wire* session_wire_{};
    std::vector<std::unique_ptr<response>> responses{};
};

};  // namespace tsubakuro::common::wire
